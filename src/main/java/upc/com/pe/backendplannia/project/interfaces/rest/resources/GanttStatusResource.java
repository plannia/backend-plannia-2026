package upc.com.pe.backendplannia.project.interfaces.rest.resources;

/**
 * Diagnóstico de la integración Gantt. Facilita el setup de Google (OAuth/carpeta) sin
 * tener que leer los logs de Azure: reporta el modo activo y, si algo falla, el motivo.
 *
 * @param enabled        gantt.enabled=true (si es false este endpoint ni existe)
 * @param mode           "google" (adapter real) o "logging" (sin Google, hojas falsas)
 * @param ready          true si Sheets+Drive inicializaron OK al arranque
 * @param reason         motivo por el que no está listo, o null si todo bien
 * @param authMode       "oauth-user", "service-account" o "none"
 * @param hasOutputFolder GANTT_OUTPUT_FOLDER_ID configurado
 * @param hasOAuthClient  GANTT_OAUTH_CLIENT_ID + SECRET configurados
 * @param hasRedirectUri  GANTT_OAUTH_REDIRECT_URI configurado (necesario para el bootstrap)
 */
public record GanttStatusResource(
        boolean enabled,
        String mode,
        boolean ready,
        String reason,
        String authMode,
        boolean hasOutputFolder,
        boolean hasOAuthClient,
        boolean hasRedirectUri
) {
}
