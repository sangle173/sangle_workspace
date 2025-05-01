<div class="card shadow-lg p-4">
    <div class="card-body">
        <h4 class="text-center mb-4">
            {{ isset($examineHistory) ? __('messages.edit_examine_history') : __('messages.add_new_examine_history') }}
        </h4>

        <form
            action="{{ isset($examineHistory) ? route('examine-histories.update', $examineHistory->id) : route('examine-histories.store') }}"
            method="POST">
        @csrf
        @if(isset($examineHistory))
            @method('PUT')
        @endif

        <!-- Patient Selection -->
            <!-- Patient Selection -->
            <div class="mb-3">
                <label for="patient_id" class="form-label">{{ __('messages.patients') }}</label>
                <select id="patient_id" name="patient_id" class="form-select">
                    <option value="">{{ __('messages.select_patient') }}</option>
                    @foreach($patients as $patient)
                        <option value="{{ $patient->id }}"
                            {{ old('patient_id', $selectedPatientId ?? $examineHistory->patient_id ?? '') == $patient->id ? 'selected' : '' }}>
                            {{ $patient->name }}
                        </option>
                    @endforeach
                </select>
                @error('patient_id')
                <div class="text-danger">{{ $message }}</div>
                @enderror
            </div>


            <!-- Diagnosis -->
            <div class="mb-3">
                <label for="diagnose" class="form-label">{{ __('messages.diagnose') }}</label>
                <input type="text" id="diagnose" name="diagnose" class="form-control"
                       value="{{ old('diagnose', $examineHistory->diagnose ?? '') }}">
                @error('diagnose')
                <div class="text-danger">{{ $message }}</div>
                @enderror
            </div>

            <!-- Symptoms -->
            <div class="mb-3">
                <label for="symptoms" class="form-label">{{ __('messages.symptoms') }}</label>
                <input type="text" id="symptoms" name="symptoms" class="form-control"
                       value="{{ old('symptoms', $examineHistory->symptoms ?? '') }}">
                @error('symptoms')
                <div class="text-danger">{{ $message }}</div>
                @enderror
            </div>

            <!-- Prescription Section -->
            <div class="row">
                <!-- Left Side: Add Medicines -->
                <div class="col-md-6">
                    <h5 class="mt-4">{{ __('messages.prescription') }}</h5>
                    <div id="prescription-container">
                        @php
                            $prescriptions = isset($examineHistory) && !empty($examineHistory->prescription)
                                ? json_decode($examineHistory->prescription, true)
                                : [];
                        @endphp

                        @foreach($prescriptions as $index => $prescription)
                            @php
                                $medicine = \App\Models\Medicine::with('unit')->find($prescription['medicine_id']);
                                $unitName = $medicine && $medicine->unit ? $medicine->unit->name : __('messages.no_unit');
                                $price = $medicine ? $medicine->price : 0;
                            @endphp

                            <div class="prescription-item d-flex align-items-center mb-2">
                                <select name="medicines[]" class="form-select medicine-search w-50" required>
                                    <option value="">{{ __('messages.select_medicine') }}</option>
                                    @foreach($medicines as $medicine)
                                        <option value="{{ $medicine->id }}"
                                                data-unit="{{ $medicine->unit ? $medicine->unit->name : __('messages.no_unit') }}"
                                                data-price="{{ $medicine->price }}"
                                            {{ $medicine->id == $prescription['medicine_id'] ? 'selected' : '' }}>
                                            {{ $medicine->name }} ({{ $medicine->unit ? $medicine->unit->name : __('messages.no_unit') }}) -
                                            ₫{{ number_format($medicine->price, 0, ',', '.') }}
                                        </option>
                                    @endforeach
                                </select>

                                <input type="number" name="quantities[]" class="form-control ms-2"
                                       style="width: 120px; height: 28px!important;"
                                       placeholder="{{ __('messages.enter_quantity') }}"
                                       value="{{ $prescription['quantity'] }}" min="1" required>

                                <button style=" height: 28px!important;" type="button" class="btn btn-danger btn-sm ms-2 remove-medicine">
                                    <i class="fas fa-trash"></i>
                                </button>
                            </div>
                        @endforeach
                    </div>

                    <button type="button" id="add-medicine" class="btn btn-primary btn-sm mt-2">
                        <i class="fas fa-plus"></i> {{ __('messages.add_medicine') }}
                    </button>
                    <!-- Save & Back Buttons -->
                    <input type="hidden" id="fee" name="fee" value="{{ old('fee', $examineHistory->fee ?? 0) }}">
                    <div class="mb-3">
                        <label for="profit" class="form-label">Lợi nhuận</label>
                        <input type="number" id="profit" name="profit" class="form-control" value="{{ old('profit', $examineHistory->profit ?? 0) }}" min="0" step="1">
                    </div>
                    <div class="d-flex gap-2 mt-4">
                        <button type="submit" class="btn btn-success btn-sm">
                            <i class="fa-solid fa-save me-1"></i> {{ __('messages.save') }}
                        </button>
                        <a href="{{ route('examine-histories.index') }}" class="btn btn-secondary btn-sm">
                            <i class="fa-solid fa-arrow-left me-1"></i> {{ __('messages.back') }}
                        </a>
                    </div>
                </div>

                <!-- Right Side: Prescription Summary -->
                <div class="col-md-6">
                    <h5 class="mt-4">{{ __('messages.prescription_details') }}</h5>
                    <table class="table table-bordered">
                        <thead>
                        <tr>
                            <th class="text-center">{{ __('messages.medicine_name') }}</th>
                            <th class="text-center">{{ __('messages.quantity') }}</th>
                            <th class="text-center">{{ __('messages.price') }}</th>
                            <th class="text-center">{{ __('messages.unit') }}</th>
                            <th class="text-center">{{ __('messages.fee') }}</th>
                        </tr>
                        </thead>
                        <tbody id="prescription-summary">
                        <!-- Prescription details will be dynamically inserted here -->
                        </tbody>
                        <tfoot>
                        <tr>
                            <td colspan="4" class="text-end"><strong>{{ __('messages.total_medicine_fee') }}</strong></td>
                            <td class="text-center"><strong><span id="total-medicine-fee">0<sup>đ</sup></span></strong></td>
                        </tr>
                        <tr>
                            <td colspan="4" class="text-end"><strong>{{ __('messages.medical_fee') }}</strong></td>
                            <td class="text-center"><strong>70,000<sup>đ</sup></strong></td>
                        </tr>
                        <tr>
                            <td colspan="4" class="text-end"><strong>{{ __('messages.final_total') }}</strong></td>
                            <td class="text-center"><strong><span id="final-total">70,000<sup>đ</sup></span></strong></td>
                        </tr>
                        </tfoot>
                    </table>
                </div>
            </div>
        </form>
    </div>
</div>

<script>
    document.addEventListener("DOMContentLoaded", function () {
        let addMedicineButton = document.getElementById("add-medicine");
        let prescriptionContainer = document.getElementById("prescription-container");
        let prescriptionSummary = document.getElementById("prescription-summary");

        let medicalFee = 70000; // Fixed medical fee

        if (!addMedicineButton || !prescriptionContainer || !prescriptionSummary) {
            console.error("Missing required elements.");
            return;
        }

        let medicines = @json($medicines);

        function generateMedicineOptions(selectedMedicineId = null) {
            let options = `<option value="">{{ __('messages.select_medicine') }}</option>`;
            medicines.forEach(med => {
                let unit = med.unit ? med.unit.name : "{{ __('messages.no_unit') }}";
                let price = med.price ? med.price : 0;
                let selected = selectedMedicineId == med.id ? 'selected' : '';

                options += `<option value="${med.id}" ${selected}
                        data-unit="${unit}" data-price="${price}">
                        ${med.name} (${unit}) - ${price.toLocaleString()}<sup>đ</sup>
                    </option>`;
            });
            return options;
        }

        function updatePrescriptionTable() {
            prescriptionSummary.innerHTML = "";
            let totalMedicineFee = 0;
            let medicalFee = 70000; // Fixed medical fee

            document.querySelectorAll(".prescription-item").forEach(row => {
                let selectElement = row.querySelector(".medicine-search");
                let quantityInput = row.querySelector("input[name='quantities[]']");
                let selectedOption = selectElement.options[selectElement.selectedIndex];

                if (!selectedOption.value) return;

                let medicineName = selectedOption.textContent;
                let unit = selectedOption.dataset.unit || "{{ __('messages.no_unit') }}";
                let price = parseFloat(selectedOption.dataset.price || 0);
                let quantity = parseInt(quantityInput.value) || 0;
                let fee = price * quantity;
                totalMedicineFee += fee;

                let newRow = `<tr>
            <td class="text-center">${medicineName}</td>
            <td class="text-center">${quantity}</td>
            <td class="text-center">${price.toLocaleString()}<sup>đ</sup></td>
            <td class="text-center">${unit}</td>
            <td class="text-center">${fee.toLocaleString()}<sup>đ</sup></td>
        </tr>`;

                prescriptionSummary.innerHTML += newRow;
            });

            // Update total medicine fee row
            document.getElementById("total-medicine-fee").innerHTML = `${totalMedicineFee.toLocaleString()}<sup>đ</sup>`;

            // Update final total row (Medical Fee + Medicine Fee)
            document.getElementById("final-total").innerHTML = `${(medicalFee + totalMedicineFee).toLocaleString()}<sup>đ</sup>`;
            // Set the hidden fee input value
            document.getElementById("fee").value = medicalFee + totalMedicineFee;
        }




        function addMedicineRow(selectedMedicineId = null, quantity = '') {
            let newMedicineRow = document.createElement("div");
            newMedicineRow.classList.add("prescription-item", "d-flex", "align-items-center", "mb-2");

            newMedicineRow.innerHTML = `
            <select name="medicines[]" class="form-select medicine-search w-50" required>
                ${generateMedicineOptions(selectedMedicineId)}
            </select>
            <input type="number" name="quantities[]" class="form-control ms-2"
                   style="width: 120px; height: 28px;"
                   placeholder="{{ __('messages.enter_quantity') }}"
                   value="${quantity}" min="1" required>
            <button style="height: 28px;" type="button" class="btn btn-danger btn-sm ms-2 remove-medicine">
                <i class="fas fa-trash"></i>
            </button>
        `;

            prescriptionContainer.appendChild(newMedicineRow);

            // Initialize select2 for new dropdowns
            $('.medicine-search').select2({
                placeholder: "{{ __('messages.select_medicine') }}",
                allowClear: true
            });

            // Attach event listeners to the new row
            newMedicineRow.querySelector(".medicine-search").addEventListener("change", updatePrescriptionTable);
            newMedicineRow.querySelector("input[name='quantities[]']").addEventListener("input", updatePrescriptionTable);

            updatePrescriptionTable();
        }

        // Ensure existing prescriptions in the edit form are initialized
        document.querySelectorAll(".prescription-item").forEach(row => {
            row.querySelector(".medicine-search").addEventListener("change", updatePrescriptionTable);
            row.querySelector("input[name='quantities[]']").addEventListener("input", updatePrescriptionTable);
        });

        // Fix: Ensure add medicine button is working
        addMedicineButton.addEventListener("click", function () {
            addMedicineRow();
        });

        // Ensure remove button works for existing and new rows
        prescriptionContainer.addEventListener("click", function (e) {
            if (e.target.closest(".remove-medicine")) {
                e.target.closest(".prescription-item").remove();
                updatePrescriptionTable();
            }
        });

        // Ensure Select2 is applied to existing medicines
        $('.medicine-search').select2({
            placeholder: "{{ __('messages.select_medicine') }}",
            allowClear: true
        });

        updatePrescriptionTable();
    });

</script>

