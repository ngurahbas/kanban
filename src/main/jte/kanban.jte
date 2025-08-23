@param boolean editKanbanTitle
@param app.kanban.kanban.KanbanWeb kanban
@param java.util.Map<String, java.util.List<app.kanban.kanban.KanbanCardWeb>> columnCards

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${kanban.getId() == null ? "New Kanban Board" : "Kanban: " + kanban.getTitle()}</title>
    <link rel="stylesheet" href="/css/main.css">
    <script src="/js/main.js" defer></script>
</head>
<body x-data="{ sidebarOpen: true }">
<nav class="navbar bg-base-100 shadow-lg">
    <div class="flex-none">
        <button class="btn btn-square btn-ghost btn-accent" @click="sidebarOpen = !sidebarOpen">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="w-6 h-6">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"/>
            </svg>
        </button>
    </div>
</nav>

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
</section>

<section id="kanbanColumns" class="flex flex-nowrap justify-center overflow-x-auto gap-2 p-4">
    @if(columnCards != null)
        @for(java.util.Map.Entry<String, java.util.List<app.kanban.kanban.KanbanCardWeb>> entry : columnCards.entrySet())
            <div class="flex-none w-100">
                <div class="card border-2 shadow hover:shadow-lg">
                    <div class="card-body p-4">
                        <h2 class="card-title text-lg">${entry.getKey()}</h2>
                        @for(app.kanban.kanban.KanbanCardWeb card : entry.getValue())
                            <div class="card bg-base-100 shadow-sm relative">
                                <div class="card-body p-3">
                                    <div class="card-title font-semibold">${card.getTitle()}</div>
                                    <p>${card.getDescription()}</p>
                                </div>
                            </div>
                        @endfor
                        <button class="btn btn-sm btn-primary w-fit self-end" onclick="document.getElementById('addCardModal').showModal()">add</button>
                    </div>
                </div>
            </div>
        @endfor
    @endif
</section>

<!-- Add Card Modal -->
<dialog id="addCardModal" class="modal">
    <div class="modal-box">
        <h3 class="font-bold text-lg">Add Card</h3>
        <p class="py-2">Fill in the details below. (Form is intentionally left empty for now.)</p>
        <form method="dialog" class="modal-action">
            <button class="btn">Close</button>
            <button class="btn btn-primary">Submit</button>
        </form>
    </div>
    <form method="dialog" class="modal-backdrop">
        <button>close</button>
    </form>
</dialog>

</body>
</html>
