package anupam.acrylic;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;

import anupam.acrylic.databinding.ActivityAboutBinding;

public class AboutActivity extends Activity {
    ActivityAboutBinding aboutBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        aboutBinding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(aboutBinding.getRoot());
        Spanned htmlText = Html.fromHtml(getResources().getString(R.string.about_description));
        aboutBinding.aboutTextView.setText(htmlText);
        aboutBinding.aboutTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
