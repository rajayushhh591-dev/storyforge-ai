
package com.storyforge.ai;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.text.InputType;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private LinearLayout root, messages;
    private EditText prompt;
    private TextView status;
    private ScrollView chatScroll;
    private Spinner genre, language;

    private SharedPreferences prefs;
    private JSONArray history;

    // Configure a secure HTTPS backend here when ready.
    private final String apiUrl = "";

    private final int bg = Color.rgb(17, 18, 29);
    private final int panel = Color.rgb(29, 30, 45);
    private final int purple = Color.rgb(139, 112, 255);
    private final int white = Color.WHITE;

    private int dp(float n) {
        return (int) (n * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("storyforge", MODE_PRIVATE);

        try {
            history = new JSONArray(
                    prefs.getString("history", "[]")
            );
        } catch (Exception e) {
            history = new JSONArray();
        }

        showHome();
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setPadding(dp(8), dp(8), dp(8), dp(8));
        return t;
    }

    private void showHome() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);
        root.setPadding(dp(14), dp(8), dp(14), dp(8));
        setContentView(root);

        // Top bar
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = text("✦ StoryForge AI", 24, white);
        title.setTypeface(null, Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        TextView menu = text("⋮", 32, white);
        menu.setGravity(Gravity.CENTER);
        menu.setPadding(dp(14), dp(4), dp(14), dp(4));
        menu.setOnClickListener(v -> showMenu(menu));
        top.addView(menu);

        root.addView(top);

        TextView subtitle = text(
                "Your imagination. Your story.",
                14, Color.LTGRAY
        );
        root.addView(subtitle);

        // Genre and language selectors
        LinearLayout settings = new LinearLayout(this);
        settings.setOrientation(LinearLayout.HORIZONTAL);

        genre = spinner(new String[]{
                "Fantasy", "Mythology", "Adventure",
                "Romance", "Thriller", "Horror", "Sci-Fi"
        });

        language = spinner(new String[]{
                "Hindi", "Hinglish", "English"
        });

        settings.addView(genre,
                new LinearLayout.LayoutParams(0, dp(48), 1));

        settings.addView(language,
                new LinearLayout.LayoutParams(0, dp(48), 1));

        root.addView(settings);

        // Chat area
        chatScroll = new ScrollView(this);
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        chatScroll.addView(messages);

        root.addView(chatScroll,
                new LinearLayout.LayoutParams(-1, 0, 1));

        // No welcome text and no placeholder text
        prompt = new EditText(this);
        prompt.setTextColor(white);
        prompt.setHint("");
        prompt.setHintTextColor(Color.LTGRAY);
        prompt.setTextSize(16);
        prompt.setGravity(Gravity.TOP | Gravity.START);
        prompt.setMinLines(3);
        prompt.setMaxLines(6);
        prompt.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );
        prompt.setPadding(dp(12), dp(12), dp(12), dp(12));
        prompt.setBackgroundColor(panel);
        root.addView(prompt);

        Button send = new Button(this);
        send.setText("Generate Story  ↗");
        send.setTextColor(white);
        send.setBackgroundTintList(
                ColorStateList.valueOf(purple)
        );

        root.addView(send,
                new LinearLayout.LayoutParams(-1, dp(52)));

        status = text("Ready", 12, Color.LTGRAY);
        root.addView(status);

        send.setOnClickListener(v -> generateStory());

        showLatestHistory();
    }

    private Spinner spinner(String[] values) {
        Spinner s = new Spinner(this);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_spinner_item,
                        values
                ) {
                    @Override
                    public View getView(
                            int position,
                            View convertView,
                            ViewGroup parent) {

                        TextView t = new TextView(MainActivity.this);
                        t.setText(getItem(position));
                        t.setTextColor(white);
                        t.setTextSize(16);
                        t.setGravity(Gravity.CENTER_VERTICAL);
                        t.setPadding(dp(8), 0, dp(8), 0);
                        return t;
                    }

                    @Override
                    public View getDropDownView(
                            int position,
                            View convertView,
                            ViewGroup parent) {

                        TextView t = new TextView(MainActivity.this);
                        t.setText(getItem(position));
                        t.setTextColor(white);
                        t.setTextSize(16);
                        t.setPadding(
                                dp(12), dp(12), dp(12), dp(12)
                        );
                        t.setBackgroundColor(panel);
                        return t;
                    }
                };

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );
        s.setAdapter(adapter);
        s.setBackgroundTintList(
                ColorStateList.valueOf(white)
        );

        return s;
    }

    private void showMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);

        popup.getMenu().add("Story History");
        popup.getMenu().add("New Story");
        popup.getMenu().add("Clear Chat");

        popup.setOnMenuItemClickListener(item -> {
            String selected = item.getTitle().toString();

            if (selected.equals("Story History")) {
                showHistory();
                return true;
            }

            if (selected.equals("New Story")) {
                newStory();
                return true;
            }

            if (selected.equals("Clear Chat")) {
                messages.removeAllViews();
                status.setText("Chat cleared");
                return true;
            }

            return false;
        });

        popup.show();
    }

    private void generateStory() {
        String idea = prompt.getText().toString().trim();

        if (idea.isEmpty()) {
            Toast.makeText(
                    this,
                    "Pehle apna story idea likho",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        addMessage("You", idea);
        prompt.setText("");

        String selectedGenre = genre.getSelectedItem().toString();
        String selectedLanguage =
                language.getSelectedItem().toString();

        askAI(idea, selectedGenre, selectedLanguage);
    }

    private void addMessage(String who, String body) {
        TextView t = text(who + "\n\n" + body, 16, white);
        t.setBackgroundColor(panel);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);

        params.setMargins(0, dp(6), 0, dp(6));
        messages.addView(t, params);

        chatScroll.post(() ->
                chatScroll.fullScroll(View.FOCUS_DOWN)
        );
    }

    private void saveStory(
            String idea,
            String answer,
            String selectedGenre,
            String selectedLanguage) {

        try {
            JSONObject item = new JSONObject();
            item.put("idea", idea);
            item.put("answer", answer);
            item.put("genre", selectedGenre);
            item.put("language", selectedLanguage);
            item.put("time", System.currentTimeMillis());

            history.put(item);

            // Keep the most recent 100 stories.
            JSONArray limited = new JSONArray();
            int start = Math.max(0, history.length() - 100);

            for (int i = start; i < history.length(); i++) {
                limited.put(history.getJSONObject(i));
            }

            history = limited;

            prefs.edit()
                    .putString("history", history.toString())
                    .apply();

        } catch (Exception e) {
            Toast.makeText(
                    this, "Story save nahi ho payi",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void showLatestHistory() {
        if (history.length() > 0) {
            addMessage(
                    "StoryForge AI",
                    "Welcome back! Apni purani stories ke liye ⋮ menu kholen."
            );
        }
    }

    private void showHistory() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(8), dp(16), dp(8));
        content.setBackgroundColor(bg);

        TextView heading = text("Story History", 24, white);
        heading.setTypeface(null, Typeface.BOLD);
        content.addView(heading);

        if (history.length() == 0) {
            content.addView(text(
                    "Abhi koi saved story nahi hai.\n" +
                    "AI se story generate karne ke baad " +
                    "woh yahan save hogi.",
                    16, Color.LTGRAY
            ));
        } else {
            for (int i = history.length() - 1; i >= 0; i--) {
                JSONObject item = history.optJSONObject(i);
                if (item == null) continue;

                final String idea = item.optString("idea");
                final String answer = item.optString("answer");
                final String savedGenre = item.optString("genre");
                final String savedLanguage =
                        item.optString("language");

                Button storyButton = new Button(this);
                storyButton.setText(
                        savedGenre + " · " + idea
                );
                storyButton.setTextColor(white);
                storyButton.setBackgroundTintList(
                        ColorStateList.valueOf(panel)
                );

                storyButton.setOnClickListener(v -> {
                    genre.setSelection(
                            findPosition(genre, savedGenre)
                    );
                    language.setSelection(
                            findPosition(language, savedLanguage)
                    );

                    messages.removeAllViews();
                    addMessage("You", idea);
                    addMessage("StoryForge AI", answer);

                    setContentView(root);
                    Toast.makeText(
                            this, "Saved story opened",
                            Toast.LENGTH_SHORT
                    ).show();
                });

                content.addView(storyButton);
            }
        }

        Button back = new Button(this);
        back.setText("Back to StoryForge");
        back.setOnClickListener(v -> setContentView(root));
        content.addView(back);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        setContentView(scroll);
    }

    private int findPosition(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString()
                    .equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private void newStory() {
        messages.removeAllViews();
        prompt.setText("");
        status.setText("Ready");

        setContentView(root);

        prompt.requestFocus();

        InputMethodManager imm =
                (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE
                );

        if (imm != null) {
            imm.showSoftInput(
                    prompt, InputMethodManager.SHOW_IMPLICIT
            );
        }
    }

    private void askAI(
            String idea,
            String selectedGenre,
            String selectedLanguage) {

        if (apiUrl.isEmpty()) {
            addMessage(
                    "StoryForge AI",
                    "AI abhi connected nahi hai. " +
                    "Secure backend configure karne ke baad " +
                    "stories generate aur history mein save hongi."
            );
            status.setText("AI connection not configured");
            return;
        }

        status.setText("Story likhi ja rahi hai...");

        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                connection = (HttpURLConnection)
                        new URL(apiUrl).openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(120000);
                connection.setDoOutput(true);
                connection.setRequestProperty(
                        "Content-Type", "application/json"
                );

                JSONObject body = new JSONObject();

                JSONArray chat = new JSONArray();
                chat.put(new JSONObject()
                        .put("role", "user")
                        .put("content", idea));

                body.put("messages", chat);
                body.put("instructions",
                        "Write in " + selectedLanguage +
                        ". Genre: " + selectedGenre +
                        ". Write an engaging story.");

                try (OutputStream out =
                             connection.getOutputStream()) {
                    out.write(body.toString().getBytes(
                            StandardCharsets.UTF_8
                    ));
                }

                int code = connection.getResponseCode();

                InputStream stream = code < 400
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                String response = readStream(stream);
                JSONObject result = new JSONObject(response);

                if (code >= 400) {
                    throw new Exception(
                            result.optString("error", "AI request failed")
                    );
                }

                String answer = result.optString(
                        "reply", "No response received"
                );

                saveStory(
                        idea, answer,
                        selectedGenre, selectedLanguage
                );

                runOnUiThread(() -> {
                    addMessage("StoryForge AI", answer);
                    status.setText("Story saved to history");
                });

            } catch (Exception e) {
                String error = e.getMessage();
                if (error == null) error = "Request failed";

                final String message = error;

                runOnUiThread(() -> {
                    addMessage("Connection error", message);
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

        if (stream == null) return "{}";

        StringBuilder result = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        stream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }

        return result.toString();
    }
}
