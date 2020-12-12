package no.hiof.museum_finder;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPhotoResponse;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;

public class FindMuseum extends Fragment {

    //This class tells you your current location
    public FusedLocationProviderClient fusedLocationProviderClient;

    //this class is responsible for loading the suggestions as you see the user type in the search string.
    public PlacesClient placesClient;

    //as the suggestions are recieved, we need a list to save those.
    public List<AutocompletePrediction> predictionList;

    public Location lastKnownLocation;

    //update userrequest if last location is null
    public LocationCallback locationCallback;

    //Search bar
    public MaterialSearchBar materialSearchBar;

    //Finds the result the user is searching for
    public TextView titleCardView;
    public TextView openingHoursCardView;
    public ImageView imageCardView;
    public TextView locationTextView;
    public TextView ratingTextView;
    private RatingBar ratingBar;
    private RequestQueue requestQueue;
    private TextView description;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("MissingPermission")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_find_museum, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        materialSearchBar = view.findViewById(R.id.searchBar);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        titleCardView = view.findViewById(R.id.titleCardView);
        openingHoursCardView = view.findViewById(R.id.openingHoursCardView);
        imageCardView = view.findViewById(R.id.imageCardView);
        locationTextView = view.findViewById(R.id.location);
        ratingTextView = view.findViewById(R.id.ratingTextView2);
        ratingBar = view.findViewById(R.id.ratingBarDetail);
        requestQueue = Volley.newRequestQueue(requireContext());
        description = view.findViewById(R.id.descriptionFindMuseum);

        Places.initialize(view.getContext(), getResources().getString(R.string.maps_api_key));
        placesClient = Places.createClient(getContext());
        final AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

        materialSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {

            }

            @Override
            public void onSearchConfirmed(CharSequence text) {

            }

            @Override
            public void onButtonClicked(int buttonCode) {
                //this function is called when you click the button on the search bar. this may be the "back" button or the hamburger menu like button
                if(buttonCode == MaterialSearchBar.BUTTON_NAVIGATION) {
                    //for example open or close navigation drawer
                } else if(buttonCode == MaterialSearchBar.BUTTON_BACK) {
                    materialSearchBar.disableSearch();
                }
            }
        });

        materialSearchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                FindAutocompletePredictionsRequest predictionsRequest = FindAutocompletePredictionsRequest.builder()
                        //Filter out what you want to search for. In this case i use ESTABLISHMENT because we want to find museums.
                        .setTypeFilter(TypeFilter.ESTABLISHMENT)
                        .setSessionToken(token)
                        .setQuery(s.toString())
                        .build();

                // You can use this to restrict search if app is only gonna be used in one country -->  .setCountry("no")

                placesClient.findAutocompletePredictions(predictionsRequest).addOnCompleteListener(new OnCompleteListener<FindAutocompletePredictionsResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<FindAutocompletePredictionsResponse> task) {
                        if(task.isSuccessful()){
                            FindAutocompletePredictionsResponse predictionsResponse = task.getResult();
                            if(predictionsResponse!=null){
                                predictionList = predictionsResponse.getAutocompletePredictions();
                                CharacterStyle s = new CharacterStyle() {
                                    @Override
                                    public void updateDrawState(TextPaint tp) {

                                    }
                                };

                                List<String> suggestionsList = new ArrayList<>();
                                for (int i = 0; i <predictionList.size() ; i++) {
                                    AutocompletePrediction prediction = predictionList.get(i);

                                    System.out.println("PLACETYPES: " + prediction.getPlaceTypes());
                                    /**
                                     * Checks if placetype is museum
                                     */
                                    for (int j = 0; j <prediction.getPlaceTypes().size() ; j++) {
                                        if (prediction.getPlaceTypes().get(j).name().equals("MUSEUM")) {
                                            suggestionsList.add(prediction.getFullText(null).toString());
                                        }
                                    }




                                }
                                materialSearchBar.updateLastSuggestions(suggestionsList);
                                if(!materialSearchBar.isSuggestionsVisible()){
                                    materialSearchBar.showSuggestionsList();
                                }
                            }
                        }else {
                            Log.i("enTag", "prediction unsuccessful");
                        }
                    }
                });
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        materialSearchBar.setSuggstionsClickListener(new SuggestionsAdapter.OnItemViewClickListener() {
            @Override
            public void OnItemClickListener(int position, View v) {
                //at this point we dont have the latitude and longitude. we only have a place id reference which the user has clicked on the search result
                //this needs to be sent to google places api and request it to return the latitude and longitude so we can find the actual address
                //and information regarding the address.

                if(position >= predictionList.size()){
                    return;
                }
                AutocompletePrediction selectedPrediction = predictionList.get(position);
                String suggestion = materialSearchBar.getLastSuggestions().get(position).toString();
                materialSearchBar.setText(suggestion);

                //Seperate thread so that the suggestion will be delayed before it gets clearaed from search list suggestion after you click it.
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        materialSearchBar.clearSuggestions();
                    }
                }, 200);

                //closes keyboard after user clicks suggestion
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if(imm != null){
                    imm.hideSoftInputFromWindow(materialSearchBar.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
                }
                String placeId = selectedPrediction.getPlaceId();
                //Here we write what we are interested in. You can chose opening hours etc.
                List<Place.Field> placeFields  = Arrays.asList(
                        Place.Field.LAT_LNG,
                        Place.Field.OPENING_HOURS,
                        Place.Field.ADDRESS,
                        Place.Field.PHONE_NUMBER,
                        Place.Field.RATING,
                        Place.Field.ADDRESS_COMPONENTS,
                        Place.Field.BUSINESS_STATUS,
                        Place.Field.PHOTO_METADATAS,
                        Place.Field.PRICE_LEVEL,
                        Place.Field.NAME,
                        Place.Field.USER_RATINGS_TOTAL,
                        Place.Field.WEBSITE_URI,
                        Place.Field.ID,
                        Place.Field.TYPES
                );

                FetchPlaceRequest fetchPlaceRequest = FetchPlaceRequest.builder(placeId, placeFields).build();

                placesClient.fetchPlace(fetchPlaceRequest).addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onSuccess(FetchPlaceResponse fetchPlaceResponse) {
                        final Place place = fetchPlaceResponse.getPlace();

                        Log.i("Tag", "place found: " + place.getName());
                        Log.i("Tag", "place opening hours: " + place.getOpeningHours());

                        LatLng latLng = place.getLatLng();
                        if(latLng != null){
                                    try {
                                    List<PhotoMetadata> metadata = place.getPhotoMetadatas();

                                    PhotoMetadata photoMetadata = metadata.get(0);

                                    // Create a FetchPhotoRequest.
                                    final FetchPhotoRequest photoRequest = FetchPhotoRequest.builder(photoMetadata)
                                            .setMaxWidth(500) // Optional.
                                            .setMaxHeight(300) // Optional.
                                            .build();
                                    placesClient.fetchPhoto(photoRequest).addOnSuccessListener(new OnSuccessListener<FetchPhotoResponse>() {
                                        @Override
                                        public void onSuccess(FetchPhotoResponse fetchPhotoResponse) {
                                            Bitmap bitmap = fetchPhotoResponse.getBitmap();
                                            imageCardView.setImageBitmap(bitmap);
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception exception) {
                                            if (exception instanceof ApiException) {
                                                final ApiException apiException = (ApiException) exception;
                                                Log.e("TAG", "Place not found: " + exception.getMessage());
                                                final int statusCode = apiException.getStatusCode();
                                                // TODO: Handle error with given status code.
                                            }
                                        }
                                    });

                                    } catch (Exception e) {
                                        Log.d("Photometadata", "Cant find photometadata");
                                        imageCardView.setBackgroundResource(R.drawable.nophoto);
                                    }

                            //openingHoursCardView.setText(Objects.requireNonNull(Objects.requireNonNull(place.getOpeningHours()).getWeekdayText()).toString());

                            LocalDate date = null;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                date = LocalDate.now().minusDays(1);
                                date.getDayOfWeek();
                            }

                            try {
                                openingHoursCardView.setText(place.getOpeningHours().getWeekdayText().get(date.getDayOfWeek().getValue()));
                            }catch (Exception e ){
                                Log.d("Tag", "Could not find opening hours");
                                openingHoursCardView.setText("Openinghours not available");
                            }

                            //System.out.println("BUSINESS ID: " + place.getId());
                            try {
                                locationTextView.setText(place.getAddress());
                                titleCardView.setText(place.getName());

                                ratingBar.setNumStars(5);
                                ratingBar.setRating(place.getRating().floatValue());
                                ratingBar.setAlpha(1);
                                ratingTextView.setText(" / " + place.getRating().toString());
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }

                            try {
                                WikiJSONParser wikiJSONParser = new WikiJSONParser();
                                wikiJSONParser.parseWikiData(place.getName(), requestQueue, description, getContext());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            /*System.out.println("Gjør det du skal her.");
                            System.out.println("Address: " + place.getAddress());
                            System.out.println("Lat Lng : " + place.getLatLng());
                            System.out.println("Opening hours: " + place.getOpeningHours());
                            System.out.println("Phone number: " + place.getPhoneNumber());
                            System.out.println("Rating: " + place.getRating());
                             */
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if(e instanceof ApiException) {
                            ApiException apiException = (ApiException) e;
                            apiException.printStackTrace();
                            int statusCode = apiException.getStatusCode();
                            Log.i("randomTag", "place not found: " + e.getMessage());
                            Log.i("randomTag", "status code: " + statusCode);
                        }
                    }
                });
            }

            @Override
            public void OnItemDeleteListener(int position, View v) {
            }
        });
    }
}