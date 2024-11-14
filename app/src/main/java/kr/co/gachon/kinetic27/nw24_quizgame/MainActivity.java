package kr.co.gachon.kinetic27.nw24_quizgame;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        SplashScreen.installSplashScreen(this);

        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String[] ipAndPort = readIpAndPortFromFile("server_info.dat");

        String ip = ipAndPort[0];
        int port = Integer.parseInt(ipAndPort[1]);

        TextView ip_text = findViewById(R.id.ip_text);
        TextView port_text = findViewById(R.id.port_text);

        ip_text.setText(ip);
        port_text.setText(String.valueOf(port));

        Button startButton = findViewById(R.id.startButton);

        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, QuizActivity.class);
            intent.putExtra("ip", ip_text.getText().toString());
            intent.putExtra("port", Integer.parseInt(port_text.getText().toString()));
            startActivity(intent);
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
}