
package com.storyforge.ai;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.text.InputType;
import android.content.res.ColorStateList;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private LinearLayout root;
    private LinearLayout messages;
    private EditText prompt;
    private TextView status;

    // Leave empty until a secure AI backend is configured.
    private final String apiUrl = "";

    private final int bg = Color.rgb(17, 18, 29);
    private final int panel = Color.rgb(29, 30, 45);
    private final int purple = Color.rgb(139, 112, 255);

    private int dp(float value) {
        return (int) (value *
                getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setPadding(dp(8), dp(8), dp(8), dp(8));
        return view;
    }

    private void showHome() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        root.setPadding(dp(16), dp(12), dp(16), dp(10));
        setContentView(root);

        TextView title = text("✦ StoryForge AI", 25, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = text(
                "Your imagination. Your story.",
                14, Color.LTGRAY
        );
        root.addView(subtitle);

        LinearLayout settings = new LinearLayout(this);
        settings.setOrientation(LinearLayout.HORIZONTAL);

        Spinner genre = spinner(new String[]{
                "Fantasy", "Mythology", "Adventure",
                "Romance", "Thriller", "Horror", "Sci-Fi"
        });

        Spinner language = spinner(new String[]{
                "Hindi", "Hinglish", "English"
        });

        settings.addView(genre,
                new LinearLayout.LayoutParams(0, dp(48), 1));

        settings.addView(language,
                new LinearLayout.LayoutParams(0, dp(48), 1));

        root.addView(settings);

        ScrollView scroll = new ScrollView(this);
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(messages);

        root.addView(scroll,
                new LinearLayout.LayoutParams(-1, 0, 1));

        TextView welcome = text(
                "Every great story starts here.\n\n" +
                "Apna idea neeche likho.",
                20, Color.WHITE
        );
        welcome.setGravity(Gravity.CENTER);
        messages.addView(welcome);

        prompt = new EditText(this);
        prompt.setTextColor(Color.WHITE);
        prompt.setHintTextColor(Color.LTGRAY);
        prompt.setHint("Describe your story or next chapter...");
        prompt.setGravity(Gravity.TOP);
        prompt.setMinLines(3);
        prompt.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE
        );
        prompt.setPadding(dp(12), dp(10), dp(12), dp(10));
        prompt.setBackgroundColor(panel);
        root.addView(prompt);

        Button send = new Button(this);
        send.setText("Generate story  ↗");
        send.setTextColor(Color.WHITE);
        send.setBackgroundTintList(
                ColorStateList.valueOf(purple)
        );

        root.addView(send,
                new LinearLayout.LayoutParams(-1, dp(52)));

        status = text(
                "Ready · AI connection needs setup",
                12, Color.LTGRAY
        );
        root.addView(status);

        send.setOnClickListener(v -> {
            String idea = prompt.getText().toString().trim();

            if (idea.isEmpty()) {
                prompt.setError("Pehle story idea likho");
                return;
            }

            addMessage("You", idea);
            prompt.setText("");

            askAI(
                    idea,
                    genre.getSelectedItem().toString(),
                    language.getSelectedItem().toString()
            );
        });
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                values
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinner.setAdapter(adapter);
        return spinner;
    }

    private void addMessage(String who, String body) {
        TextView message = text(
                who + "\n\n" + body,
                16, Color.WHITE
        );

        message.setBackgroundColor(panel);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);

        params.setMargins(0, dp(6), 0, dp(6));
        messages.addView(message, params);

        if (messages.getParent() instanceof ScrollView) {
            ((ScrollView) messages.getParent()).post(
                    () -> ((ScrollView) messages.getParent())
                            .fullScroll(View.FOCUS_DOWN)
            );
        }
    }

    private void askAI(
            String idea,
            String genre,
            String language
    ) {
        status.setText("Story likhi ja rahi hai...");

        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                if (apiUrl.isEmpty()) {
                    runOnUiThread(() -> {
                        addMessage(
                                "StoryForge AI",
                                "AI abhi connect nahi hai. " +
                                "AI story generation ke liye " +
                                "secure backend configure karna hoga."
                        );
                        status.setText("AI connection not configured");
                    });
                    return;
                }

                URL url = new URL(apiUrl);
                connection = (HttpURLConnection)
                        url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(120000);
                connection.setDoOutput(true);
                connection.setRequestProperty(
                        "Content-Type", "application/json"
                );

                JSONObject body = new JSONObject();

                JSONArray conversation = new JSONArray();
                conversation.put(
                        new JSONObject()
                                .put("role", "user")
                                .put("content", idea)
                );

                body.put("messages", conversation);
                body.put(
                        "instructions",
                        "Write in " + language +
                        ". Genre: " + genre +
                        ". Write an engaging story and maintain continuity."
                );

                try (OutputStream output =
                             connection.getOutputStream()) {
                    output.write(
                            body.toString().getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );
                }

                int responseCode = connection.getResponseCode();

                InputStream stream = responseCode < 400
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                String response = readStream(stream);
                JSONObject result = new JSONObject(response);

                if (responseCode >= 400) {
                    throw new Exception(
                            result.optString(
                                    "error", "AI request failed"
                            )
                    );
                }

                String answer = result.optString(
                        "reply", "No response received"
                );

                runOnUiThread(() -> {
                    addMessage("StoryForge AI", answer);
                    status.setText("Story generated");
                });

            } catch (Exception error) {
                String message = error.getMessage();

                if (message == null || message.isEmpty()) {
                    message = "AI request failed";
                }

                final String errorMessage = message;

                runOnUiThread(() -> {
                    addMessage("Connection error", errorMessage);
                    status.setText("Connection failed");
                });

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private String readStream(InputStream stream)
            throws IOException {

        if (stream == null) {
            return "{}";
        }

        StringBuilder result = new StringBuilder();

        try (
                InputStream input = stream;
                InputStreamReader reader =
                        new InputStreamReader(
                                input, StandardCharsets.UTF_8
                        );
                BufferedReader buffered =
                        new BufferedReader(reader)
        ) {
            String line;

            while ((line = buffered.readLine()) != null) {
                result.append(line);
            }
        }

        return result.toString();
    }
}
