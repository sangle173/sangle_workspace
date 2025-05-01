{{--@extends('layouts.app')--}}

{{--@section('content')--}}
{{--    <h1>{{ __('messages.add_new') }} {{ __('messages.examine_histories') }}</h1>--}}
{{--    <form action="{{ route('examine-histories.store') }}" method="POST">--}}
{{--        @csrf--}}
{{--        @include('examine_histories.form', ['history' => null])--}}
{{--    </form>--}}
{{--@endsection--}}
@extends('layouts.app')

@section('content')
    @include('examine_histories.form', ['examineHistory' => null, 'medicines' => $medicines, 'patients' => $patients])
@endsection
