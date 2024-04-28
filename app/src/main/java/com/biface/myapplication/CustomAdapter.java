package com.biface.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.MyViewHolder> {

    Activity activity;
    ArrayList<String> perguntas, respostas;
    int positionAnimation = -1;
    int position;

    public CustomAdapter(Activity activity, ArrayList perguntas, ArrayList respostas){
        this.activity = activity;
        this.perguntas = perguntas;
        this.respostas = respostas;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_chat, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        //this.position = position;

        holder.lblPergunta.setText(String.valueOf(perguntas.get(position)));
        //se a pergunta não teve animação
        if(positionAnimation < position) {
            holder.lblResposta.animateText(String.valueOf(respostas.get(position)));
            positionAnimation = position;
        }else{
            holder.lblResposta.setText(String.valueOf(respostas.get(position)));
        }
    }

    @Override
    public int getItemCount() {
        return respostas.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{
        TextView lblPergunta;
        TypeWriter lblResposta;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            lblPergunta = itemView.findViewById(R.id.lblSend);
            lblResposta = itemView.findViewById(R.id.lblReceive);
        }
    }
}
