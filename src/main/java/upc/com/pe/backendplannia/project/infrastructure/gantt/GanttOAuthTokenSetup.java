package upc.com.pe.backendplannia.project.infrastructure.gantt;

import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.SheetsScopes;

import java.util.List;

/**
 * One-time utility to obtain a refresh token for personal Gmail (OAuth user mode).
 * <p>
 * Run from the project root:
 * {@code mvn -q exec:java -Dexec.mainClass=upc.com.pe.backendplannia.project.infrastructure.gantt.GanttOAuthTokenSetup -Dexec.args="CLIENT_ID CLIENT_SECRET"}
 */
public final class GanttOAuthTokenSetup {
    private static final List<String> SCOPES = List.of(
            SheetsScopes.SPREADSHEETS,
            DriveScopes.DRIVE,
            DriveScopes.DRIVE_FILE
    );

    private GanttOAuthTokenSetup() {
    }

    public static void main(String[] args) throws Exception {
        var clientId = firstNonBlank(args, 0, "GANTT_OAUTH_CLIENT_ID");
        var clientSecret = firstNonBlank(args, 1, "GANTT_OAUTH_CLIENT_SECRET");
        if (clientId == null || clientSecret == null) {
            System.err.println("Usage: GanttOAuthTokenSetup <clientId> <clientSecret>");
            System.err.println("Or set env vars GANTT_OAUTH_CLIENT_ID and GANTT_OAUTH_CLIENT_SECRET.");
            System.exit(1);
        }

        var transport = GoogleNetHttpTransport.newTrustedTransport();
        var jsonFactory = GsonFactory.getDefaultInstance();
        var flow = new GoogleAuthorizationCodeFlow.Builder(transport, jsonFactory, clientId, clientSecret, SCOPES)
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();

        var receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        var redirectUri = receiver.getRedirectUri();
        var authorizationUrl = new GoogleAuthorizationCodeRequestUrl(clientId, redirectUri, SCOPES)
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();

        System.out.println("Open this URL in your browser and sign in with the Gmail account that owns the Gantt folder:");
        System.out.println(authorizationUrl);
        System.out.println();
        System.out.println("Waiting for authorization on http://localhost:8888 ...");

        var code = receiver.waitForCode();
        var tokenResponse = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();
        if (tokenResponse.getRefreshToken() == null || tokenResponse.getRefreshToken().isBlank()) {
            System.err.println("No refresh token returned. Revoke app access at https://myaccount.google.com/permissions and run again.");
            System.exit(2);
        }

        System.out.println();
        System.out.println("Add these App Settings in Azure (or application-dev.properties):");
        System.out.println("GANTT_OAUTH_CLIENT_ID=" + clientId);
        System.out.println("GANTT_OAUTH_CLIENT_SECRET=" + clientSecret);
        System.out.println("GANTT_OAUTH_REFRESH_TOKEN=" + tokenResponse.getRefreshToken());
        System.out.println("GANTT_OUTPUT_FOLDER_ID=<folder id from your Gmail My Drive, e.g. plannia folder>");
        System.out.println("GANTT_GOOGLE_ENABLED=true");
        System.out.println();
        System.out.println("You can remove GANTT_GOOGLE_CREDENTIALS_JSON when using OAuth user mode.");
    }

    private static String firstNonBlank(String[] args, int index, String envName) {
        if (args != null && args.length > index && args[index] != null && !args[index].isBlank()) {
            return args[index].trim();
        }
        var env = System.getenv(envName);
        return env == null || env.isBlank() ? null : env.trim();
    }
}
