package no.hiof.museum_finder;

import android.view.View;
import android.widget.ToggleButton;

public interface CardViewClickManager {
    void onCardViewClick(int position, View v);
    void onCardViewClick(int position, View v, String distance);
    void onCardViewToggleButtonCheckedChanged(int position, ToggleButton favourite, boolean isChecked);
}
