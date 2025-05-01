<section>
    <header class="mb-4">
        <h2 class="h5 mb-1">Thông tin cá nhân</h2>
        <p class="text-muted small mb-0">Cập nhật thông tin cá nhân và địa chỉ email của bạn.</p>
    </header>

    <form id="send-verification" method="post" action="{{ route('verification.send') }}">
        @csrf
    </form>

    <form method="post" action="{{ route('profile.update') }}">
        @csrf
        @method('patch')

        <div class="mb-3">
            <label for="name" class="form-label">Họ và tên</label>
            <input id="name" name="name" type="text" class="form-control @error('name') is-invalid @enderror" value="{{ old('name', $user->name) }}" required autofocus autocomplete="name">
            @error('name')
                <div class="invalid-feedback">{{ $message }}</div>
            @enderror
        </div>

        <div class="mb-3">
            <label for="email" class="form-label">Email</label>
            <input id="email" name="email" type="email" class="form-control @error('email') is-invalid @enderror" value="{{ old('email', $user->email) }}" required autocomplete="username">
            @error('email')
                <div class="invalid-feedback">{{ $message }}</div>
            @enderror

            @if ($user instanceof \Illuminate\Contracts\Auth\MustVerifyEmail && ! $user->hasVerifiedEmail())
                <div class="alert alert-warning mt-2 p-2">
                    <p class="mb-1 small">Địa chỉ email của bạn chưa được xác minh.
                        <button form="send-verification" class="btn btn-link btn-sm p-0 align-baseline">Bấm vào đây để gửi lại email xác minh.</button>
                    </p>
                    @if (session('status') === 'verification-link-sent')
                        <div class="alert alert-success mt-2 mb-0 p-2 small">
                            Một liên kết xác minh mới đã được gửi đến email của bạn.
                        </div>
                    @endif
                </div>
            @endif
        </div>

        <div class="d-flex align-items-center gap-3">
            <button type="submit" class="btn btn-primary">Lưu</button>
            @if (session('status') === 'profile-updated')
                <div class="alert alert-success mb-0 py-1 px-2 small">Đã lưu.</div>
            @endif
        </div>
    </form>
</section>
