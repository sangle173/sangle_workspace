<section>
    <header class="mb-4">
        <h2 class="h5 mb-1">Xóa tài khoản</h2>
        <p class="text-muted small mb-0">Khi tài khoản của bạn bị xóa, tất cả dữ liệu và tài nguyên sẽ bị xóa vĩnh viễn. Vui lòng tải về các thông tin bạn muốn giữ lại trước khi xóa tài khoản.</p>
    </header>

    <!-- Trigger Button -->
    <button type="button" class="btn btn-danger" data-bs-toggle="modal" data-bs-target="#confirmUserDeletionModal">
        Xóa tài khoản
    </button>

    <!-- Bootstrap Modal -->
    <div class="modal fade" id="confirmUserDeletionModal" tabindex="-1" aria-labelledby="confirmUserDeletionModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <form method="post" action="{{ route('profile.destroy') }}">
                    @csrf
                    @method('delete')
                    <div class="modal-header">
                        <h5 class="modal-title" id="confirmUserDeletionModalLabel">Bạn có chắc chắn muốn xóa tài khoản?</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                    </div>
                    <div class="modal-body">
                        <p class="mb-2">Khi tài khoản bị xóa, tất cả dữ liệu và tài nguyên sẽ bị xóa vĩnh viễn. Vui lòng nhập mật khẩu để xác nhận xóa tài khoản.</p>
                        <div class="mb-3">
                            <label for="delete-user-password" class="form-label sr-only">Mật khẩu</label>
                            <input id="delete-user-password" name="password" type="password" class="form-control @error('password', 'userDeletion') is-invalid @enderror" placeholder="Mật khẩu">
                            @error('password', 'userDeletion')
                                <div class="invalid-feedback">{{ $message }}</div>
                            @enderror
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-danger">Xóa tài khoản</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</section>
