package kr.co.gachon.kinetic27.nw24_quizgame;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class QuizActivity extends AppCompatActivity {
    BufferedReader in;
    PrintWriter out;

    TextView quizText, quizLengthText, answerText;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.quiz_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        quizText = findViewById(R.id.quiz_text);
        quizLengthText = findViewById(R.id.quiz_text_size);
        answerText = findViewById(R.id.answer_text);

        Intent intent = getIntent();
        String ip = intent.getStringExtra("ip");
        int port = intent.getIntExtra("port", 1234);

        Log.i("ip, port", ip + ", " + port);

        Button submitButton = findViewById(R.id.submit_button);

        // connect to server
        openSocket(ip, port);

        submitButton.setOnClickListener(v -> {
            String answer = answerText.getText().toString();
            Log.i("answer", answer);

            if (!answer.isEmpty()) {
                String encoded_answer = URLEncoder.encode(answer, StandardCharsets.UTF_8);
                Log.i("answer", encoded_answer);

                InputMethodManager manager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                manager.hideSoftInputFromWindow(Objects.requireNonNull(getCurrentFocus()).getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                new Thread(() -> {
                    out.println("q_answer:" + encoded_answer);

                    readLineFromServer();
                }).start();
            }
        });

        answerText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                submitButton.callOnClick();
            }

            return false;
        });
    }

    public void openSocket(String ip, int port) {
        new Thread(() -> {
            try {
                // noinspection resource
                Socket clientSocket = new Socket(ip, port);

                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                readLineFromServer();
            } catch (IOException e) {
                Log.e("QuizActivity", Objects.requireNonNull(e.getMessage()));
                Log.e("QuizActivity", "Server Not Ready");

                runOnUiThread(() -> Toast.makeText(this, "Server Not Ready", Toast.LENGTH_SHORT).show());
                finish();
            }
        }).start();
    }

    @SuppressLint("SetTextI18n")
    public void readLineFromServer() {
        try {
            String serverMsg = in.readLine();

            if (serverMsg.startsWith("quiz:")) {
                String[] quiz = serverMsg.split(":");

                runOnUiThread(() -> {
                    quizText.setText(quiz[1]);
                    quizLengthText.setText(quiz[2] + "글자");
                });
            }

            // if server send answer is correct
            if (serverMsg.startsWith("answer:")) {
                String[] cmd = serverMsg.split(":");

                runOnUiThread(() -> {
                    if (Objects.equals(cmd[1], "correct")) {
                        Snackbar.make(findViewById(R.id.quiz_main), "정답입니다!", Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(findViewById(R.id.quiz_main), "오답입니다!", Snackbar.LENGTH_SHORT).show();
                    }

                    answerText.setText("");
                });

                // call recursively
                readLineFromServer();
            }

            // if server message is show score
            if (serverMsg.startsWith("score:")) {
                int score = Integer.parseInt(serverMsg.split(":")[1]);

                runOnUiThread(() -> {
                    findViewById(R.id.submit_button).setEnabled(false);
                    answerText.setEnabled(false);

                    quizText.setText(score + "점 입니다!");
                    quizLengthText.setText("수고하셨습니다.");

                    new Handler().postDelayed(() -> {
                        Snackbar.make(findViewById(R.id.quiz_main),
                                "최종 점수는 " + score + "점 입니다.", Snackbar.LENGTH_SHORT
                        ).show();

                        new Handler().postDelayed(QuizActivity.this::finish, 1200);
                    }, 1000);
                });
            }
        } catch (IOException e) {
            Log.e("QuizActivity", Objects.requireNonNull(e.getMessage()));
            Log.e("QuizActivity", "Server Error");

            QuizActivity.this.finish();
        }
    }
}