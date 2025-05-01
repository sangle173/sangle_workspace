<nav class="navbar navbar-expand-lg navbar-dark bg-dark mb-4 shadow-sm">
    <div class="container">
        <a class="navbar-brand d-flex align-items-center gap-2" href="{{ route('examine-histories.index') }}">
            <i class="fa-solid fa-house-medical"></i>
            <span>{{ __('messages.clinic') }}</span>
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav" aria-controls="navbarNav" aria-expanded="false" aria-label="{{ __('messages.toggle_navigation') }}">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
            <ul class="navbar-nav me-auto">
                <li class="nav-item">
                    <a class="nav-link d-flex align-items-center gap-1 {{ request()->is('patients*') ? 'active fw-bold' : '' }}" href="{{ route('patients.index') }}">
                        <i class="fa-solid fa-user-injured"></i> {{ __('messages.patients') }}
                    </a>
                </li>
              
                <li class="nav-item">
                    <a class="nav-link d-flex align-items-center gap-1 {{ request()->is('examine-histories*') ? 'active fw-bold' : '' }}" href="{{ route('examine-histories.index') }}">
                        <i class="fa-solid fa-notes-medical"></i> {{ __('messages.examine_histories') }}
                    </a>
                </li>
                <!-- Medicine Management Dropdown -->
                <li class="nav-item dropdown">
                    <a class="nav-link dropdown-toggle d-flex align-items-center gap-1 {{ request()->is('medicines*') || request()->is('medicine-statuses*') || request()->is('units*') || request()->is('brands*') || request()->is('medicine-categories*') ? 'active fw-bold' : '' }}" href="#" id="medicineDropdown" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                        <i class="fa-solid fa-pills"></i> {{ __('messages.medicine_management') }}
                    </a>
                    <ul class="dropdown-menu" aria-labelledby="medicineDropdown">
                        <li><a class="dropdown-item d-flex align-items-center gap-1 {{ request()->is('medicines*') ? 'active fw-bold' : '' }}" href="{{ route('medicines.index') }}"><i class="fa-solid fa-capsules"></i> {{ __('messages.medicines') }}</a></li>
                        <li><a class="dropdown-item d-flex align-items-center gap-1 {{ request()->is('medicine-categories*') ? 'active fw-bold' : '' }}" href="{{ route('medicine-categories.index') }}"><i class="fa-solid fa-layer-group"></i> {{ __('messages.medicine_categories') }}</a></li>
                        <li><a class="dropdown-item d-flex align-items-center gap-1 {{ request()->is('medicine-statuses*') ? 'active fw-bold' : '' }}" href="{{ route('medicine-statuses.index') }}"><i class="fa-solid fa-clipboard-check"></i> {{ __('messages.medicine_status') }}</a></li>
                        <li><a class="dropdown-item d-flex align-items-center gap-1 {{ request()->is('units*') ? 'active fw-bold' : '' }}" href="{{ route('units.index') }}"><i class="fa-solid fa-ruler-combined"></i> {{ __('messages.units') }}</a></li>
                        <li><a class="dropdown-item d-flex align-items-center gap-1 {{ request()->is('brands*') ? 'active fw-bold' : '' }}" href="{{ route('brands.index') }}"><i class="fa-solid fa-copyright"></i> {{ __('messages.brands') }}</a></li>
                    </ul>
                </li>
                <li class="nav-item">
                    <a class="nav-link d-flex align-items-center gap-1 {{ request()->is('addresses*') ? 'active fw-bold' : '' }}" href="{{ route('addresses.index') }}">
                        <i class="fa-solid fa-location-dot"></i> {{ __('messages.addresses') }}
                    </a>
                </li>
            </ul>
            <ul class="navbar-nav ms-auto">
                @guest
                    <li class="nav-item">
                        <a class="nav-link d-flex align-items-center gap-1" href="{{ route('login') }}"><i class="fa-solid fa-right-to-bracket"></i> {{ __('Login') }}</a>
                    </li>
                    @if (Route::has('register'))
                        <li class="nav-item">
                            <a class="nav-link d-flex align-items-center gap-1" href="{{ route('register') }}"><i class="fa-solid fa-user-plus"></i> {{ __('Register') }}</a>
                        </li>
                    @endif
                @else
                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle d-flex align-items-center gap-2" href="#" id="userDropdown" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                            <i class="fa-solid fa-user-circle"></i> <span>{{ Auth::user()->name }}</span>
                        </a>
                        <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="userDropdown">
                            <li><a class="dropdown-item d-flex align-items-center gap-1" href="{{ route('profile.edit') }}"><i class="fa-solid fa-user-gear"></i> {{ __('Profile') }}</a></li>
                            <li><hr class="dropdown-divider"></li>
                            <li>
                                <form method="POST" action="{{ route('logout') }}">
                                    @csrf
                                    <button type="submit" class="dropdown-item d-flex align-items-center gap-1"><i class="fa-solid fa-right-from-bracket"></i> {{ __('Log Out') }}</button>
                                </form>
                            </li>
                        </ul>
                    </li>
                @endguest
            </ul>
        </div>
    </div>
</nav>
