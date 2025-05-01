<?php

namespace App\Http\Controllers;

use App\Models\ExamineHistory;
use App\Models\Medicine;
use App\Models\Patient;
use Illuminate\Http\Request;

class ExamineHistoryController extends Controller
{
    /**
     * Display a listing of examine histories.
     */
    public function index(Request $request)
    {
        $startDate = $request->input('start_date', now()->format('Y-m-d'));
        $endDate = $request->input('end_date', now()->format('Y-m-d'));

        $summaryQuery = ExamineHistory::whereBetween('created_at', [
            $startDate . ' 00:00:00',
            $endDate . ' 23:59:59'
        ]);

        $summary = [
            'total' => $summaryQuery->count(),
            'total_fee' => $summaryQuery->sum('fee'),
            'total_profit' => $summaryQuery->sum('profit'),
        ];

        $search = $request->input('search');
        $examineHistories = ExamineHistory::query()
            ->with('patient')
            ->when($search, function ($query, $search) {
                $query->whereHas('patient', function ($q) use ($search) {
                    $q->where('name', 'like', '%' . $search . '%');
                });
            })
            ->whereBetween('created_at', [$startDate . ' 00:00:00', $endDate . ' 23:59:59'])
            ->orderBy('created_at', 'desc')
            ->paginate(10);

        $today = now()->format('Y-m-d');
        $weekStart = \Carbon\Carbon::now()->startOfWeek(\Carbon\Carbon::MONDAY)->format('Y-m-d');
        $weekEnd = \Carbon\Carbon::now()->endOfWeek(\Carbon\Carbon::MONDAY)->format('Y-m-d');
        $monthStart = \Carbon\Carbon::now()->startOfMonth()->format('Y-m-d');
        $monthEnd = \Carbon\Carbon::now()->endOfMonth()->format('Y-m-d');

        return view('examine_histories.index', compact(
            'examineHistories', 'search', 'summary', 'startDate', 'endDate',
            'today', 'weekStart', 'weekEnd', 'monthStart', 'monthEnd'
        ));
    }


    /**
     * Show the form for creating a new examine history.
     */
    public function create(Request $request)
    {
        $selectedPatientId = $request->query('patient_id'); // Get patient_id from URL

        return view('examine_histories.create', [
            'patients' => Patient::where('status', 1)->get(),
            'medicines' => Medicine::where('status', 1)->with('unit')->get(),
            'selectedPatientId' => $selectedPatientId, // Pass to view
        ]);
    }




    /**
     * Store a newly created examine history in the database.
     */
    public function store(Request $request)
    {
        $validatedData = $request->validate([
            'patient_id' => 'required|exists:patients,id',
            'diagnose' => 'nullable|string|max:255',
            'symptoms' => 'nullable|string|max:255',
            'medicines' => 'required|array', // Ensure medicines are an array
            'medicines.*' => 'exists:medicines,id', // Validate each medicine ID
            'quantities' => 'required|array', // Ensure quantities are an array
            'quantities.*' => 'string|max:50', // Ensure each quantity is valid
            'fee' => 'nullable|integer|min:0',
            'profit' => 'nullable|integer|min:0',
        ]);

        // Format prescription into JSON
        $prescription = [];
        foreach ($request->medicines as $key => $medicineId) {
            $prescription[] = [
                'medicine_id' => $medicineId,
                'quantity' => $request->quantities[$key]
            ];
        }

        ExamineHistory::create([
            'patient_id' => $validatedData['patient_id'],
            'diagnose' => $validatedData['diagnose'] ?? '',
            'symptoms' => $validatedData['symptoms'] ?? '',
            'prescription' => !empty($prescription) ? json_encode($prescription) : json_encode([]), // Store as JSON
            'fee' => $validatedData['fee'] ?? 0,
            'profit' => $validatedData['profit'] ?? 0,
        ]);

        return redirect()->route('examine-histories.index')->with('success', __('messages.examine_history_created'));
    }




    /**
     * Show the form for editing the specified examine history.
     */
    public function edit(ExamineHistory $examineHistory)
    {
        return view('examine_histories.edit', [
            'examineHistory' => $examineHistory,
            'patients' => Patient::where('status', 1)->get(),
            'medicines' => Medicine::where('status', 1)->with('unit')->get(), // Include unit details
        ]);
    }


    /**
     * Update the specified examine history in the database.
     */
    public function update(Request $request, ExamineHistory $examineHistory)
    {
        $validatedData = $request->validate([
            'patient_id' => 'required|exists:patients,id',
            'diagnose' => 'nullable|string|max:255',
            'symptoms' => 'nullable|string|max:255',
            'medicines' => 'required|array',
            'medicines.*' => 'exists:medicines,id',
            'quantities' => 'required|array',
            'quantities.*' => 'string|max:50',
            'fee' => 'nullable|integer|min:0',
            'profit' => 'nullable|integer|min:0',
        ]);

        // Format prescription into JSON
        $prescription = [];
        foreach ($request->medicines as $key => $medicineId) {
            $prescription[] = [
                'medicine_id' => $medicineId,
                'quantity' => $request->quantities[$key]
            ];
        }

        $examineHistory->update([
            'patient_id' => $validatedData['patient_id'],
            'diagnose' => $validatedData['diagnose'] ?? '',
            'symptoms' => $validatedData['symptoms'] ?? '',
            'prescription' => !empty($prescription) ? json_encode($prescription) : json_encode([]), // Store as JSON
            'fee' => $validatedData['fee'] ?? 0,
            'profit' => $validatedData['profit'] ?? 0,
        ]);

        return redirect()->route('examine-histories.index')->with('success', __('messages.examine_history_updated'));
    }


    public function indexByPatient($patientId)
    {
        // Fetch the patient
        $patient = Patient::findOrFail($patientId);

        // Fetch examine histories for the patient ordered by ID (descending)
        $examineHistories = ExamineHistory::query()
            ->where('patient_id', $patientId)
            ->orderBy('id', 'desc')
            ->get();

        // Pass variables to the view
        return view('examine_histories.show', compact('examineHistories', 'patient'));
    }


    /**
     * Soft delete the specified examine history (set status = 0).
     */
    public function destroy(ExamineHistory $examineHistory)
    {
        $examineHistory->update(['status' => 0]); // Soft delete
        return redirect()->route('examine-histories.index')->with('success', 'Examine history deleted successfully.');
    }
}
