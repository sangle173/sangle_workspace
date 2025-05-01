{{--@extends('layouts.app')--}}

{{--@section('content')--}}
{{--    <h1>Edit Examine History</h1>--}}
{{--    <form action="{{ route('examine-histories.update', $examineHistory) }}" method="POST">--}}
{{--        @csrf--}}
{{--        @method('PUT')--}}
{{--        @include('examine_histories.form', ['history' => $examineHistory])--}}
{{--    </form>--}}
{{--@endsection--}}
@extends('layouts.app')

@section('content')
    @include('examine_histories.form', ['examineHistory' => $examineHistory, 'medicines' => $medicines, 'patients' => $patients])
@endsection
