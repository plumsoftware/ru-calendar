package com.plumsoftware.rucalendar.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.plumsoftware.rucalendar.R;
import com.plumsoftware.rucalendar.activities.EventActivity;
import com.plumsoftware.rucalendar.events.CelebrationItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CelebrationAdapter extends RecyclerView.Adapter<CelebrationHolder> {
    protected Context context;
    protected Activity activity;
    protected List<CelebrationItem> celebrations;
    //protected RewardedAd mRewardedAd;

    public CelebrationAdapter(Context context, Activity activity, List<CelebrationItem> celebrations) {
        this.context = context;
        this.activity = activity;
        this.celebrations = celebrations;
    }

    @NonNull
    @Override
    public CelebrationHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CelebrationHolder(LayoutInflater.from(context).inflate(R.layout.recycler_view_item, parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull CelebrationHolder holder, @SuppressLint("RecyclerView") int position) {
        if (position == 0) {
            holder.textViewDay.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(new Date(celebrations.get(position).getTimeInMillis())));
            String s = new SimpleDateFormat("MMMM", Locale.getDefault()).format(new Date(celebrations.get(position).getTimeInMillis()));
            String s2 = new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date(celebrations.get(position).getTimeInMillis()));
            holder.textViewMonth.setText(s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1));
            holder.textViewClickDate.setText(s2.substring(0, 1).toUpperCase(Locale.ROOT) + s2.substring(1));
        } else {
            holder.textViewDay.setVisibility(View.GONE);
            holder.textViewMonth.setVisibility(View.GONE);
            holder.textViewClickDate.setVisibility(View.GONE);
            holder.imageViewDivider.setVisibility(View.GONE);
        }

        if (!celebrations.get(position).getName().isEmpty()) {

            //mRewardedAd = new RewardedAd(context);
            //mRewardedAd.setAdUnitId("R-M-2215793-3");

            holder.textViewName.setText(celebrations.get(position).getName());
            holder.textViewDesc.setText(celebrations.get(position).getDesc());
            if (!celebrations.get(position).getColor().isEmpty()) {
                //holder.imageViewPromo.setBackgroundColor(Color.parseColor(celebrations.get(position).getColor()));
                //holder.bCard.setCardBackgroundColor(Color.parseColor(celebrations.get(position).getColor()));


                holder.cardViewBig.setCardBackgroundColor(Color.parseColor(celebrations.get(position).getColor()));
            }

            holder.bTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Создание объекта таргетирования рекламы.
                    //final AdRequest adRequest = new AdRequest.Builder().build();
                    //MainActivity.swipeRefreshLayout.setRefreshing(true);

                    Intent intent = new Intent(context, EventActivity.class);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.putExtra("event", celebrations.get(position));
                    } else {
                        intent.putExtra("time", celebrations.get(position).getTimeInMillis());
                        intent.putExtra("name", celebrations.get(position).getName());
                        intent.putExtra("desc", celebrations.get(position).getDesc());
//                        intent.putExtra("color", celebrations.get(position).getColor());
                    }
                    activity.startActivity(intent);

                    // Регистрация слушателя для отслеживания событий, происходящих в рекламе.
//                    mRewardedAd.setRewardedAdEventListener(new RewardedAdEventListener() {
//                        @Override
//                        public void onRewarded(@NonNull final Reward reward) {
//                            //MainActivity.swipeRefreshLayout.setRefreshing(false);
//                            @SuppressLint("InflateParams") View viewLayout = (ConstraintLayout) activity.getLayoutInflater().inflate(R.layout.dialog_layout, null);
//                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
//                            builder.setView(viewLayout);
//                            builder.setCancelable(true);
//                            TextView textViewD = (TextView) viewLayout.findViewById(R.id.textDescription);
//                            textViewD.setText(celebrations.get(position).getDesc());
//                            AlertDialog alertDialog = builder.create();
//                            alertDialog.show();
//                        }
//
//                        @Override
//                        public void onAdClicked() {
//                            progressDialog.dismiss();
////                                MainActivity.swipeRefreshLayout.setRefreshing(false);
//                        }
//
//                        @Override
//                        public void onAdLoaded() {
////                                MainActivity.swipeRefreshLayout.setRefreshing(false);
//                            progressDialog.dismiss();
//                            mRewardedAd.show();
//                        }
//
//                        @Override
//                        public void onAdFailedToLoad(@NonNull final AdRequestError adRequestError) {
//                            //Toast.makeText(context, adRequestError.toString(), Toast.LENGTH_SHORT).show();
//                            progressDialog.dismiss();
//                            @SuppressLint("InflateParams") View viewLayout = (ConstraintLayout) activity.getLayoutInflater().inflate(R.layout.dialog_layout, null);
//                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
//                            builder.setView(viewLayout);
//                            builder.setCancelable(true);
//                            TextView textViewD = (TextView) viewLayout.findViewById(R.id.textDescription);
//                            textViewD.setText(celebrations.get(position).getDesc());
//                            AlertDialog alertDialog = builder.create();
//                            alertDialog.show();
//                        }
//
//                        @Override
//                        public void onAdShown() {
//                            progressDialog.dismiss();
//                        }
//
//                        @Override
//                        public void onAdDismissed() {
//                            progressDialog.dismiss();
//                        }
//
//                        @Override
//                        public void onLeftApplication() {
//                            progressDialog.dismiss();
//                        }
//
//                        @Override
//                        public void onReturnedToApplication() {
//
//                        }
//
//                        @Override
//                        public void onImpression(@Nullable ImpressionData impressionData) {
//
//                        }
//                    });
//
//                    // Загрузка объявления.
//                    mRewardedAd.loadAd(adRequest);
//                    MainActivity.rewardedCount++;
                }
            });
        } else {
            holder.cardViewBig.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return celebrations.size();
    }
}

class CelebrationHolder extends RecyclerView.ViewHolder {
    protected TextView textViewName, textViewDesc, textViewClickDate, bTextView, textViewDay, textViewMonth;
    protected ImageView imageViewPromo, imageViewDivider;
    protected CardView bCard, cardViewBig;

    public CelebrationHolder(@NonNull View itemView) {
        super(itemView);
        textViewName = (TextView) itemView.findViewById(R.id.textViewName);
        textViewDesc = (TextView) itemView.findViewById(R.id.textViewDesc);
        textViewDay = (TextView) itemView.findViewById(R.id.textViewDay);
        textViewMonth = (TextView) itemView.findViewById(R.id.textViewMonth);
        textViewClickDate = (TextView) itemView.findViewById(R.id.textViewClickDate);
        bTextView = (TextView) itemView.findViewById(R.id.bTextView);
        imageViewPromo = (ImageView) itemView.findViewById(R.id.imageViewColor);
        imageViewDivider = (ImageView) itemView.findViewById(R.id.imageViewDivider);
        bCard = (CardView) itemView.findViewById(R.id.bCard);
        cardViewBig = (CardView) itemView.findViewById(R.id.cardViewBig);
    }
}
