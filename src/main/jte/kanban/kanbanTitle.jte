@param boolean editKanbanTitle
@param app.kanban.kanban.KanbanWeb kanban
@param boolean kanbanCreated
@param java.util.Map<String, java.util.List<app.kanban.kanban.KanbanCardWeb>> columnCards

<section id="kanbanTitleSection" class="p-4">
    @if(editKanbanTitle)
        <form hx-post="/kanban/title" hx-target="#kanbanTitleSection" hx-swap="outerHTML">
            <fieldset class="fieldset flex items-center justify-center">
                <label for="kanbanTitle" class="text-3xl">Title</label>
                <input value="${kanban.getId() != null ? String.valueOf(kanban.getId()) : ""}" type="hidden" name="id">
                <input value="${kanban.getTitle()}" type="text" name="title" id="kanbanTitle" class="input input-info w-2xl text-3xl">
                <button class="btn btn-primary btn-md">${kanban.getId() == null ? "Create" : "Update"}</button>
            </fieldset>
        </form>
    @else
        <form hx-get="/kanban/${kanban.getId()}/edit" hx-target="#kanbanTitleSection" hx-swap="outerHTML">
            <fieldset class="fieldset flex items-center justify-center group">
                <h1 class="text-3xl">${kanban.getTitle()}</h1>
                <input value="${kanban.getTitle()}" type="hidden" name="title">
                <button class="btn btn-dash opacity-0 group-hover:opacity-100 transition-opacity duration-200">Edit</button>
            </fieldset>
        </form>
    @endif

    @if(kanbanCreated)
        <section hx-swap-oob="true" id="kanbanColumns" class="flex flex-nowrap justify-center overflow-x-auto gap-2 p-4">
            @for(java.util.Map.Entry<String, java.util.List<app.kanban.kanban.KanbanCardWeb>> entry : columnCards.entrySet())
                <div class="flex-none w-100">
                    <div class="card border-2 shadow hover:shadow-lg">
                        <div class="card-body p-4">
                            <h2 class="card-title text-lg">${entry.getKey()}</h2>
                            <button class="btn btn-sm btn-primary w-fit self-end" onclick="document.getElementById('addCardModal').showModal()">add</button>
                        </div>
                    </div>
                </div>
            @endfor
        </section>
    @endif
</section>
