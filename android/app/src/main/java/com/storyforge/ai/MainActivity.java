package com.storyforge.ai;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.;
import android.text.InputType;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
LinearLayout root, messages;
EditText prompt;
TextView status;
String apiUrl = "";
StringBuilder conversation = new StringBuilder();

int bg = Color.rgb(17,18,29);
int panel = Color.rgb(29,30,45);
int purple = Color.rgb(139,112,255);

int dp(float n) {
    return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
}

@Override
public void onCreate(Bundle b) {
    super.onCreate(b);
    showHome();
}

TextView text(String value, int size, int color) {
    TextView t = new TextView(this);
    t.setText(value);
    t.setTextSize(size);
    t.setTextColor(color);
    t.setPadding(dp(8),dp(8),dp(8),dp(8));
    return t;
}

void showHome() {
    root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(bg);
    root.setPadding(dp(16),dp(12),dp(16),dp(10));
    setContentView(root);

    TextView title = text("✦ StoryForge AI", 25, Color.WHITE);
    title.setTypeface(null, Typeface.BOLD);
    root.addView(title);

    TextView subtitle = text("Your imagination. Your story.", 14,
            Color.LTGRAY);
    root.addView(subtitle);

    LinearLayout settings = new LinearLayout(this);
    settings.setOrientation(LinearLayout.HORIZONTAL);

    Spinner genre = spinner(new String[]{"Fantasy","Mythology",
            "Adventure","Romance","Thriller","Horror","Sci-Fi"});
    Spinner language = spinner(new String[]{"Hindi","Hinglish","English"});
    settings.addView(genre,new LinearLayout.LayoutParams(0,dp(48),1));
    settings.addView(language,new LinearLayout.LayoutParams(0,dp(48),1));
    root.addView(settings);

    ScrollView scroll = new ScrollView(this);
    messages = new LinearLayout(this);
    messages.setOrientation(LinearLayout.VERTICAL);
    scroll.addView(messages);
    root.addView(scroll,new LinearLayout.LayoutParams(
            -1,0,1));

    TextView welcome = text(
            "Every great story starts here.\n\nApna idea neeche likho.",
            20,Color.WHITE);
    welcome.setGravity(Gravity.CENTER);
    messages.addView(welcome);

    prompt = new EditText(this);
    prompt.setTextColor(Color.WHITE);
    prompt.setHintTextColor(Color.LTGRAY);
    prompt.setHint("Describe your story or next chapter...");
    prompt.setGravity(Gravity.TOP);
    prompt.setMinLines(3);
    prompt.setInputType(InputType.TYPE_CLASS_TEXT |
            InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    prompt.setPadding(dp(12),dp(10),dp(12),dp(10));
    prompt.setBackgroundColor(panel);
    root.addView(prompt);

    Button send = new Button(this);
    send.setText("Generate story  ↗");
    send.setTextColor(Color.WHITE);
    send.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(purple));
    root.addView(send,new LinearLayout.LayoutParams(-1,dp(52)));

    status = text("Ready · AI connection needs setup",12,Color.LTGRAY);
    root.addView(status);

    send.setOnClickListener(v -> {
        String idea = prompt.getText().toString().trim();
        if (idea.isEmpty()) {
            prompt.setError("Pehle story idea likho");
            return;
        }
        addMessage("You",idea);
        prompt.setText("");
        askAI(idea, genre.getSelectedItem().toString(),
                language.getSelectedItem().toString());
    });
}

Spinner spinner(String[] values) {
    Spinner s = new Spinner(this);
    ArrayAdapter<String> a = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item,values);
    s.setAdapter(a);
    return s;
}

void addMessage(String who, String body) {
    TextView t = text(who + "\n\n" + body,16,Color.WHITE);
    t.setBackgroundColor(panel);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            -1,-2);
    p.setMargins(0,dp(6),0,dp(6));
    messages.addView(t,p);
}

void askAI(String idea, String genre, String language) {
    status.setText("Story likhi ja rahi hai...");
    new Thread(() -> {
        HttpURLConnection c = null;
        try {
            if (apiUrl.isEmpty()) {
                runOnUiThread(() -> {
                    addMessage("StoryForge",
                      "AI abhi connect nahi hai. Pehle secure AI backend URL configure karna hoga.");
                    status.setText("AI connection not configured");
                });
                return;
            }

            URL url = new URL(apiUrl);
            c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(20000);
            c.setReadTimeout(120000);
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type","application/json");

            JSONObject body = new JSONObject();
            body.put("messages",new JSONArray()
                    .put(new JSONObject()
                    .put("role","user").put("content",idea)));
            body.put("instructions","Write in "+language+
                    ". Genre: "+genre+
                    ". Write an engaging story and maintain continuity.");

            try (OutputStream out = c.getOutputStream()) {
                out.write(body.toString().getBytes(
                        StandardCharsets.UTF_8));
            }

            InputStream stream = c.getResponseCode() < 400
                    ? c.getInputStream() : c.getErrorStream();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            stream.transferTo(bytes);
            JSONObject result = new JSONObject(
                    bytes.toString(StandardCharsets.UTF_8));
            if (c.getResponseCode() >= 400) {
                throw new Exception(result.optString("error",
                        "AI request failed"));
            }

            String answer = result.optString("reply",
                    "No response received");
            runOnUiThread(() -> {
                addMessage("StoryForge AI",answer);
                status.setText("Story generated");
            });

        } catch (Exception e) {
            runOnUiThread(() -> {
                addMessage("Connection error",
                        e.getMessage() == null
                        ? "AI request failed" : e.getMessage());
                status.setText("Connection failed");
            });
        } finally {
            if (c != null) c.disconnect();
        }
    }).start();
}

}
