package upc.com.pe.backendplannia.project.application.internal.eventhandlers;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import upc.com.pe.backendplannia.project.domain.model.commands.RemoveCategoryMemberFromAllCategoriesCommand;
import upc.com.pe.backendplannia.project.domain.services.CategoryCommandService;
import upc.com.pe.backendplannia.shared.domain.model.events.UserDeletedEvent;

@Component
public class UserDeletedEventHandler {
    private final CategoryCommandService categoryCommandService;

    public UserDeletedEventHandler(CategoryCommandService categoryCommandService) {
        this.categoryCommandService = categoryCommandService;
    }

    @EventListener
    public void on(UserDeletedEvent event) {
        categoryCommandService.handle(new RemoveCategoryMemberFromAllCategoriesCommand(event.userId()));
    }
}
