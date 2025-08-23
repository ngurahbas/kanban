@param app.kanban.kanban.KanbanCardWeb card
@param long kanbanId
@param String column

<div class="card bg-base-100 shadow-sm relative">
    <div class="card-body p-3">
        <div class="card-title font-semibold">${card.getTitle()}</div>
        <p>${card.getDescription()}</p>
    </div>
</div>
