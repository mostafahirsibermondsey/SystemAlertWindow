package in.jvapps.system_alert_window;

import android.content.Context;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;

public class BubbleActivity extends AppCompatActivity {

    private  LinearLayout bubbleLayout;
    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bubble);
        bubbleLayout = findViewById(R.id.bubbleLayout);
    }

}
