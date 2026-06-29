package upc.com.pe.backendplannia.project.infrastructure.gantt;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;

import java.io.IOException;

final class GoogleApiIOExceptionHelper {
    private GoogleApiIOExceptionHelper() {
    }

    static String describe(IOException exception) {
        if (exception instanceof GoogleJsonResponseException googleError) {
            var details = googleError.getDetails();
            if (details != null && details.getMessage() != null && !details.getMessage().isBlank()) {
                return details.getMessage();
            }
            return "HTTP " + googleError.getStatusCode() + " " + googleError.getStatusMessage();
        }
        if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
            return exception.getMessage();
        }
        return exception.getClass().getSimpleName();
    }
}
