<!-- resources/views/partials/_tinymce_script.blade.php -->
<script src="https://cdn.tiny.cloud/1/i54t02nln4onlhw34pkcm1ticlzhaxb98pn8s7c73w7t9ehv/tinymce/6/tinymce.min.js" referrerpolicy="origin"></script>

<script>
tinymce.init({
    selector: '.tiny-editor',
    plugins: 'image link media lists table',
    toolbar: 'undo redo | styleselect | bold italic underline | alignleft aligncenter alignright alignjustify | bullist numlist | outdent indent | link image media',
    images_upload_url: '{{ route('notes.uploadImage') }}',
    images_upload_credentials: true,
    setup: function (editor) {
        editor.on('change', function () {
            editor.save();  // ✅ This line syncs TinyMCE content into <textarea>
        });
    }
});
</script>
