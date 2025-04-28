<div class="p-2">
    <h5>Notes in {{ $notebook->name }}</h5>
    <ul class="list-group">
        @foreach($notes as $note)
            <a href="{{ route('notes.edit', $note->id) }}" class="list-group-item list-group-item-action">
                {{ $note->title }}
            </a>
        @endforeach
    </ul>
</div>
