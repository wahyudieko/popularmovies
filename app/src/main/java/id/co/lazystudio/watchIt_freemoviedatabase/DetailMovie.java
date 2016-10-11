package id.co.lazystudio.watchIt_freemoviedatabase;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import id.co.lazystudio.watchIt_freemoviedatabase.adapter.SummaryMovieAdapter;
import id.co.lazystudio.watchIt_freemoviedatabase.adapter.VideoAdapter;
import id.co.lazystudio.watchIt_freemoviedatabase.connection.TmdbClient;
import id.co.lazystudio.watchIt_freemoviedatabase.connection.TmdbService;
import id.co.lazystudio.watchIt_freemoviedatabase.entity.Collection;
import id.co.lazystudio.watchIt_freemoviedatabase.entity.Company;
import id.co.lazystudio.watchIt_freemoviedatabase.entity.Genre;
import id.co.lazystudio.watchIt_freemoviedatabase.entity.Image;
import id.co.lazystudio.watchIt_freemoviedatabase.entity.Keyword;
import id.co.lazystudio.watchIt_freemoviedatabase.entity.Movie;
import id.co.lazystudio.watchIt_freemoviedatabase.entity.Video;
import id.co.lazystudio.watchIt_freemoviedatabase.parser.MovieParser;
import id.co.lazystudio.watchIt_freemoviedatabase.utils.FabVisibilityChangeListener;
import id.co.lazystudio.watchIt_freemoviedatabase.utils.Utils;
import retrofit2.Call;
import retrofit2.Response;

public class DetailMovie extends AppCompatActivity {
    public static final String MOVIE_KEY = "movie";
    private Movie mMovie;
    private Collection mCollection = null;
    private List<Company> mCompanyList = new ArrayList<>();
    private List<Genre> mGenreList = new ArrayList<>();
    private ArrayList<Image> mBackdropList = new ArrayList<>();
    private ArrayList<Image> mPosterList = new ArrayList<>();
    private List<Keyword> mKeywordList = new ArrayList<>();
    private List<Video> mVideo = new ArrayList<>();
    private List<Movie> mSimilarList = new ArrayList<>();
    private ProgressBar detailProgressBar;
    private TextView mNotificationTextView;

    ImageView backdropImageView;
    ImageView posterImageView;
    SystemBarTintManager tintManager;

    FloatingActionButton refreshFab;
    FabVisibilityChangeListener fabListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_movie);

        Bundle args = getIntent().getExtras();
        mMovie = args.getParcelable(MOVIE_KEY);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(mMovie.getTitle());

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow(); // in Activity's onCreate() for instance
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }

        tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setNavigationBarTintEnabled(true);

        RelativeLayout.LayoutParams toolbarParams = (RelativeLayout.LayoutParams) findViewById(R.id.stub_statusbar).getLayoutParams();
        toolbarParams.height = getStatusBarHeight();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            toolbar.setTitleTextColor(getColorWithAlpha(0, getResources().getColor(android.R.color.white, getTheme())));
        else
            toolbar.setTitleTextColor(getColorWithAlpha(0, getResources().getColor(android.R.color.white)));

        final ScrollView detailScrollView = ((ScrollView) findViewById(R.id.movie_detail_scrollview));
        detailScrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                float alpha = (float) detailScrollView.getScrollY() / backdropImageView.getBottom();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    tintManager.setTintColor(getColorWithAlpha(alpha, getResources().getColor(R.color.colorPrimary, getTheme())));
                    toolbar.setTitleTextColor(getColorWithAlpha(alpha, getResources().getColor(android.R.color.white, getTheme())));
                    toolbar.setBackgroundColor(getColorWithAlpha(alpha, getResources().getColor(R.color.colorPrimary, getTheme())));
                }else {
                    tintManager.setTintColor(getColorWithAlpha(alpha, getResources().getColor(R.color.colorPrimary)));
                    toolbar.setTitleTextColor(getColorWithAlpha(alpha, getResources().getColor(android.R.color.white)));
                    toolbar.setBackgroundColor(getColorWithAlpha(alpha, getResources().getColor(R.color.colorPrimary)));
                }
            }
        });

        detailProgressBar = (ProgressBar) findViewById(R.id.detail_movie_progressbar);

        mNotificationTextView = (TextView) findViewById(R.id.notification_textview);

        backdropImageView = (ImageView) findViewById(R.id.backdrop_imageview);

        refreshFab = (FloatingActionButton) findViewById(R.id.refresh_fab);
        fabListener = new FabVisibilityChangeListener();

        getMovie();
        firstPopulateView();
        Utils.initializeAd(this, detailScrollView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    private void getMovie(){
        if(Utils.isInternetConnected(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                tintManager.setTintColor(getColorWithAlpha(0, getResources().getColor(R.color.colorPrimary, getTheme())));
            }else {
                tintManager.setTintColor(getColorWithAlpha(0, getResources().getColor(R.color.colorPrimary)));
            }

            TmdbService tmdbService =
                    TmdbClient.getClient().create(TmdbService.class);

            final Call<MovieParser> movie = tmdbService.getMovie(mMovie.getId());

            movie.enqueue(new retrofit2.Callback<MovieParser>() {
                @Override
                public void onResponse(Call<MovieParser> call, Response<MovieParser> response) {
                    MovieParser movieParser = response.body();
                    if(response.code() != 200){
                        setComplete("Server Error Occurred");
                    }else{
                        mMovie = response.body();
                        mCollection = movieParser.getCollection();
                        mCompanyList = movieParser.getCompanies();
                        mGenreList = movieParser.getGenres();
                        mBackdropList = (ArrayList<Image>) movieParser.getBackdrops();
                        mPosterList = (ArrayList<Image>) movieParser.getPosters();
                        mKeywordList = movieParser.getKeywords();
                        mVideo = movieParser.getVideos();
                        mSimilarList = movieParser.getSimilars();
                        populateView();
                        setComplete();

                        backdropImageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent i = new Intent(DetailMovie.this, ListBackdropActivity.class);
                                i.putParcelableArrayListExtra(ListBackdropActivity.KEY_BACKDROP_LIST, mBackdropList);
                                i.putExtra(ListBackdropActivity.KEY_TITLE, mMovie.getTitle());
                                startActivity(i);
                            }
                        });

                        posterImageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent i = new Intent(DetailMovie.this, ListPosterActivity.class);
                                i.putParcelableArrayListExtra(ListPosterActivity.KEY_POSTER_LIST, mPosterList);
                                i.putExtra(ListPosterActivity.KEY_TITLE, mMovie.getTitle());
                                startActivity(i);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(Call<MovieParser> call, Throwable t) {
                    t.printStackTrace();
                    setComplete("Server Error Occurred");
                }
            });
        }else {
            setComplete("No Internet Connection");
        }
    }

    private void firstPopulateView(){
        /* BACKDROP */
        backdropImageView.post(new Runnable() {
            @Override
            public void run() {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) backdropImageView.getLayoutParams();
                params.height = backdropImageView.getWidth() * 9 / 16;

                Utils.createRipple(backdropImageView);
            }
        });

        Picasso.with(this)
                .load(mMovie.getBackdropPath(this, 1))
                .error(R.drawable.no_image_land)
                .into(backdropImageView, new Callback() {
                    @Override
                    public void onSuccess() {
                        findViewById(R.id.backdrop_progressbar).setVisibility(View.GONE);
                    }

                    @Override
                    public void onError() {
                        findViewById(R.id.backdrop_progressbar).setVisibility(View.GONE);
                    }
                });

        /* POSTER */
        posterImageView = (ImageView) findViewById(R.id.poster_imageview);
        posterImageView.post(new Runnable() {
            @Override
            public void run() {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) posterImageView.getLayoutParams();
                params.height = posterImageView.getWidth() * 3 / 2;

                LinearLayout container = (LinearLayout) findViewById(R.id.content_container);
                RelativeLayout.LayoutParams containerParams = (RelativeLayout.LayoutParams) container.getLayoutParams();
                containerParams.setMargins(0, -((params.height / 3)), 0, 0);

                FrameLayout movieFrameLayout = (FrameLayout) findViewById(R.id.movie_title_framelayout);
                LinearLayout.LayoutParams titleParams = (LinearLayout.LayoutParams) movieFrameLayout.getLayoutParams();
                titleParams.setMargins(0, (params.height / 3), 0, 0);

                View titleView = findViewById(R.id.movie_title_view);
                RelativeLayout.LayoutParams viewParams = (RelativeLayout.LayoutParams)titleView.getLayoutParams();
                viewParams.height = movieFrameLayout.getHeight()*2;
                viewParams.setMargins(0, (params.height / 3), 0, 0);

                Utils.createRipple(posterImageView);
            }
        });

        Picasso.with(this)
                .load(mMovie.getPosterPath(this, 0))
                .error(R.drawable.no_image_port)
                .fit()
                .centerCrop()
                .into(posterImageView, new Callback() {
                    @Override
                    public void onSuccess() {
                        findViewById(R.id.poster_progressbar).setVisibility(View.GONE);
                    }

                    @Override
                    public void onError() {
                        findViewById(R.id.poster_progressbar).setVisibility(View.GONE);
                    }
                });

        TextView titleTextView = ((TextView) findViewById(R.id.movie_title_textview));
        titleTextView.setText(mMovie.getTitle());
    }

    private void populateView(){
        findViewById(R.id.detail_container).setVisibility(View.VISIBLE);
        /* TITLE + RATE + POPULARITY + BUDGET + REVENUE */
        TextView releaseDateTextView = ((TextView) findViewById(R.id.movie_releasedate_textview));
        releaseDateTextView.setVisibility(View.VISIBLE);
        releaseDateTextView.setText(mMovie.getReleaseDate());
        TextView runtimeTextView = ((TextView) findViewById(R.id.movie_runtime_textview));
        runtimeTextView.setVisibility(View.VISIBLE);
        runtimeTextView.setText(mMovie.getRuntime());
        TextView rateAvgTextView = ((TextView) findViewById(R.id.movie_rateaverage_textview));
        rateAvgTextView.setText(mMovie.getVoteAverage());
        TextView rateCountTextView = ((TextView) findViewById(R.id.movie_ratecount_textview));
        if(mMovie.getVoteCount() > 0){
            rateCountTextView.setVisibility(View.VISIBLE);
            rateCountTextView.setText(String.valueOf(mMovie.getVoteCount()));
        }
        TextView budgetTextView = (TextView) findViewById(R.id.movie_budget_textview);
        budgetTextView.setText(mMovie.getBudget());
        TextView revenueTextView = (TextView) findViewById(R.id.movie_revenue_textview);
        revenueTextView.setText(mMovie.getRevenue());

        ((TextView) findViewById(R.id.movie_popularity_textview)).setText(mMovie.getPopularity());

        /* TAGLINE */
        if(mMovie.getTagline() != null) {
            if(!mMovie.getTagline().equals("")) {
                final RelativeLayout taglineRelativeLayout = (RelativeLayout) findViewById(R.id.movie_tagline_relativelayout);
                taglineRelativeLayout.setVisibility(View.VISIBLE);
                taglineRelativeLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        final TextView taglineTextView = ((TextView) findViewById(R.id.movie_tagline_textview));
                        taglineTextView.setText(mMovie.getTagline());
                        taglineTextView.setMaxWidth(taglineRelativeLayout.getWidth() - (2 * getResources().getDimensionPixelSize(R.dimen.tag_width)));
                    }
                });
            }
        }

        /* OVERVIEW */
        if(mMovie.getOverview() != null){
            if(!mMovie.getOverview().equals("")){
                findViewById(R.id.movie_overview_relativelayout).setVisibility(View.VISIBLE);
                TextView overviewTextView = (TextView) findViewById(R.id.movie_overview_textview);
                overviewTextView.setText(mMovie.getOverview());
            }
        }

        /* GENRE */
        if(mGenreList.size() > 0){
            FlexboxLayout tagFlexboxLayout = (FlexboxLayout) findViewById(R.id.genre_flexboxlayout);
            tagFlexboxLayout.setVisibility(View.VISIBLE);

            for(int i = 0; i < mGenreList.size(); i++){
                Genre genre = mGenreList.get(i);
                View view = LayoutInflater.from(this).inflate(R.layout.item_genre_detail, tagFlexboxLayout, false);
                TextView genreTextView =((TextView) view.findViewById(R.id.genre_text_view));
                tagFlexboxLayout.addView(view);
                genreTextView.setText(genre.getName());
                genreTextView.setTag(i);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        View genreTextView = view.findViewById(R.id.genre_text_view);
                        Integer index = (Integer) genreTextView.getTag();
                        Genre genre = mGenreList.get(index);
                        Log.e("genre clicked", genre.getId()+" - "+genre.getName());
                        Intent i = new Intent(DetailMovie.this, ListMovie.class);
                        i.putExtra(ListMovie.GENRE, true);
                        i.putExtra(ListMovie.KEY_ID, genre.getId());
                        i.putExtra(ListMovie.KEY_TITLE, genre.getName());
                        startActivity(i);
                    }
                });
            }
        }

        /* COLLECTION */
        if(mCollection != null){
            final RelativeLayout collectionRelativeLayout = (RelativeLayout) findViewById(R.id.movie_collection_relativelayout);
            collectionRelativeLayout.setVisibility(View.VISIBLE);

            final ImageView collectionImageView = (ImageView) findViewById(R.id.movie_collection_imageview);

            collectionImageView.post(new Runnable() {
                @Override
                public void run() {
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) collectionImageView.getLayoutParams();
                    params.width = collectionRelativeLayout.getWidth();
                    params.height = collectionRelativeLayout.getWidth() / 3;
                    Picasso.with(DetailMovie.this)
                            .load(mCollection.getBackdropPath(DetailMovie.this, 0))
                            .error(R.drawable.no_image_land)
                            .resize(collectionRelativeLayout.getWidth(), collectionRelativeLayout.getWidth()/3)
                            .centerCrop()
                            .into(collectionImageView, new Callback() {
                                @Override
                                public void onSuccess() {
                                    findViewById(R.id.movie_collection_progressbar).setVisibility(View.GONE);
                                    TextView collectionTextView = (TextView) findViewById(R.id.movie_collection_textview);
                                    collectionTextView.setVisibility(View.VISIBLE);
                                    collectionTextView.setText(mCollection.getName());
                                }

                                @Override
                                public void onError() {
                                    findViewById(R.id.movie_collection_progressbar).setVisibility(View.GONE);
                                    TextView collectionTextView = (TextView) findViewById(R.id.movie_collection_textview);
                                    collectionTextView.setVisibility(View.VISIBLE);
                                    collectionTextView.setText(mCollection.getName());
                                }
                            });
                }
            });

            collectionRelativeLayout.getChildAt(0).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.e("collection", mCollection.getName());
                    Intent i = new Intent(DetailMovie.this, ListMovie.class);
                    i.putExtra(ListMovie.COLLECTION, true);
                    i.putExtra(ListMovie.KEY_ID, mCollection.getId());
                    i.putExtra(ListMovie.KEY_TITLE, mCollection.getName());
                    startActivity(i);
                }
            });
        }

        /* KEYWORD */
        if(mKeywordList.size() > 0){
            findViewById(R.id.movie_keyword_relativelayout).setVisibility(View.VISIBLE);

            FlexboxLayout keywordFlexboxLayout = (FlexboxLayout) findViewById(R.id.keyword_flexboxlayout);

            for(int i = 0; i < mKeywordList.size(); i++){
                Keyword keyword = mKeywordList.get(i);
                View view = LayoutInflater.from(this).inflate(R.layout.item_keyword_detail, keywordFlexboxLayout, false);
                TextView keywordTextView =((TextView) view.findViewById(R.id.keyword_text_view));
                keywordFlexboxLayout.addView(view);
                keywordTextView.setText(keyword.getName());
                keywordTextView.setTag(i);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        View keywordTextView = view.findViewById(R.id.keyword_text_view);
                        Integer index = (Integer) keywordTextView.getTag();
                        Keyword keyword = mKeywordList.get(index);
                        Log.e("keyword clicked", keyword.getId()+" - "+keyword.getName());
                        Intent i = new Intent(DetailMovie.this, ListMovie.class);
                        i.putExtra(ListMovie.KEYWORD, true);
                        i.putExtra(ListMovie.KEY_ID, keyword.getId());
                        i.putExtra(ListMovie.KEY_TITLE, keyword.getName());
                        startActivity(i);
                    }
                });
            }
        }

        /* SIMILAR */
        if(mSimilarList.size() > 0){
            findViewById(R.id.movie_similar_relativelayout).setVisibility(View.VISIBLE);
            mSimilarList.add(new Movie(-1));

            final RecyclerView similarRecyclerView = (RecyclerView) findViewById(R.id.movie_similar_recyclerview);


            final RelativeLayout parent = (RelativeLayout)similarRecyclerView.getParent();

            parent.post(new Runnable() {
                @Override
                public void run() {
                    RelativeLayout.LayoutParams similarParams = new RelativeLayout.LayoutParams(similarRecyclerView.getWidth(), similarRecyclerView.getWidth() / 2);
                    similarParams.addRule(RelativeLayout.BELOW, R.id.movie_similar_label_textview);
                    similarRecyclerView.setLayoutParams(similarParams);
                    similarRecyclerView.setAdapter(new SummaryMovieAdapter(DetailMovie.this, mSimilarList, ListMovie.SIMILAR, mMovie));
                }
            });

            findViewById(R.id.movie_similar_label_textview).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(DetailMovie.this, ListMovie.class);
                    i.putExtra(ListMovie.SIMILAR, true);
                    i.putExtra(ListMovie.KEY_ID, mMovie.getId());
                    i.putExtra(ListMovie.KEY_TITLE, mMovie.getTitle());
                    startActivity(i);
                }
            });
        }

        /* VIDEO */
        if(mVideo.size() > 0){
            findViewById(R.id.movie_video_relativelayout).setVisibility(View.VISIBLE);
            final GridView videoGridView = (GridView) findViewById(R.id.movie_video_gridview);

            videoGridView.setAdapter(new VideoAdapter(this, mVideo));

        }
    }

    private void setComplete(){
        Utils.setProcessComplete(detailProgressBar);
    }

    private void setComplete(String error){
        setComplete();
        Utils.setProcessError(mNotificationTextView, error);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            tintManager.setTintColor(getResources().getColor(R.color.colorAccent, getTheme()));
        else
            tintManager.setTintColor(getResources().getColor(R.color.colorAccent));
        fabListener.setFabShouldBeShown(true);
        refreshFab.show(fabListener);
        refreshFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fabListener.setFabShouldBeShown(false);
                refreshFab.hide(fabListener);
                mNotificationTextView.setVisibility(View.GONE);
                detailProgressBar.setVisibility(View.VISIBLE);
                getMovie();
            }
        });
    }

    public static int getColorWithAlpha(float alpha, int baseColor) {
        int a = Math.min(255, Math.max(0, (int) (alpha * 255))) << 24;
        int rgb = 0x00ffffff & baseColor;
        return a + rgb;
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
