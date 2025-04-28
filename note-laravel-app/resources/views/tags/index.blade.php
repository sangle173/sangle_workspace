<div class="p-2 mt-4">
    <h5>Tags</h5>
    <ul class="list-group">
        @foreach($tags as $tag)
            <li class="list-group-item">{{ $tag->name }}</li>
        @endforeach
    </ul>
</div>
