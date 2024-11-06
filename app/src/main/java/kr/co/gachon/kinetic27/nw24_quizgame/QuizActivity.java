package kr.co.gachon.kinetic27.nw24_quizgame;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

    TextView quizText, answerText;


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
        answerText = findViewById(R.id.answer_text);

        String[] ipAndPort = readIpAndPortFromFile("server_info.dat");

        String ip = ipAndPort[0];
        int port = Integer.parseInt(ipAndPort[1]);

        Log.i("ip, port", ip + ", " + port);


        Button submitButton = findViewById(R.id.submit_button);

        // connect to server
        new Thread(() -> {
            openSocket(ip, port);
        }).start();

        submitButton.setOnClickListener(v -> {
            String answer = answerText.getText().toString();
            Log.i("answer", answer);
            if (!answer.isEmpty()) {
                String encoded_answer = URLEncoder.encode(answer, StandardCharsets.UTF_8);
                Log.i("answer", encoded_answer);

                new Thread(() -> {
                    out.println("q_answer:" + encoded_answer);
                    readServer();
                }).start();
            }
        });

        answerText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    submitButton.callOnClick();
                }

                return false;
            }
        });
    }


    public String[] readIpAndPortFromFile(String filePath) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open(filePath)))) {
            String ip = br.readLine();
            String port = br.readLine();

            return new String[]{ip, port};
        } catch (IOException | NumberFormatException e) {
            return new String[]{"localhost", "1234"};
        }
    }

    public void openSocket(String ip, int port) {
        try {
            // noinspection resource
            Socket clientSocket = new Socket(ip, port);

            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            readServer();
        } catch (IOException e) {
            Log.i("QuizActivity", Objects.requireNonNull(e.getMessage()));
            Log.i("QuizActivity", "Server Not Ready");

            finish();
        }
    }

    public void readServer() {
        try {
            String serverMsg = in.readLine();

            if (serverMsg.startsWith("quiz:")) {
                String[] quiz = serverMsg.split(":");
                runOnUiThread(() -> quizText.setText(quiz[1]));
                // Integer.parseInt(quiz[2])
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
                    readServer();
                });
            }

            // if server message is show score
            if (serverMsg.startsWith("score:")) {
                int score = Integer.parseInt(serverMsg.split(":")[1]);


                runOnUiThread(() -> {
                    findViewById(R.id.submit_button).setEnabled(false);

                    new Handler().postDelayed(() -> {
                        Snackbar.make(findViewById(R.id.quiz_main),
                                "최종 점수는 " + score + "점 입니다.", Snackbar.LENGTH_SHORT
                        ).show();

                        new Handler().postDelayed(this::finish, 1200);
                    }, 1000);
                });
                // 결과창 이동
                //                clientSocket.close();
            }
        } catch (IOException e) {
            Log.e("QuizActivity", Objects.requireNonNull(e.getMessage()));
            Log.e("QuizActivity", "Server Error");

            finish();
        }
    }
}