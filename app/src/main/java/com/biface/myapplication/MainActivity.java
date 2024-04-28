package com.biface.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private boolean falarResposta;

    private EditText txtNumber1;
    private EditText txtNumber2;
    private TextView lblInicial;
    private TextView lblResultado;
    private ImageButton btnCalcular;

    private EditText txtSpeachText;
    private TextToSpeech textToSpeech;

    private Button[] botoesAtalhos;

    private static final int RECOGNIZER_RESULT = 1;

    ArrayList<String> perguntas, respostas;
    RecyclerView recyclerView;
    CustomAdapter customAdapter;

    boolean temAlgoNoEdiText;
    boolean foiDigitado; // digitado = true; falado = false

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar3);
        //setSupportActionBar(toolbar);
        //MainActivity.this.setTitle("Porcentagem Fácil");

        iniciarFalador();
        falarResposta = true;
        txtNumber1 = findViewById(R.id.txt_number1);
        txtNumber2 = findViewById(R.id.txt_number2);
        lblInicial = findViewById(R.id.lbl_info_inicial);
        btnCalcular = findViewById(R.id.btnCalcular);
        txtSpeachText = findViewById(R.id.txtSpeachText);
        botoesAtalhos = new Button[] {
                findViewById(R.id.btnReal),
                findViewById(R.id.btnPercent),
                findViewById(R.id.btnAdd),
                findViewById(R.id.btnSub),
                findViewById(R.id.btnEqual)};
        String[] charRespectiveButton = { "R$", "%", "+", "-", "="};

/*
*   erros
*   ao inserir R$ na posicao inicial antes de qualquer letra
*   ao inserir R$R$
* */
        for (int i = 0; i < botoesAtalhos.length; i++) {
            int finalI = i;
            botoesAtalhos[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Editable text = txtSpeachText.getText();
                    int cursorPosition = txtSpeachText.getSelectionStart();

                    //se for o botao R$ e tiver algo no edittext
                    if(charRespectiveButton[finalI].equals("R$") && text.length() > 0){
                        //se a ultima letra for digito e que nao anteceda de $
                        //(?<!\$)\s-?[0-9\.,]+$
                        Matcher m = Pattern.compile("-?[0-9.,]+$").matcher(text.toString().substring(0, cursorPosition));
                        if(m.find()){
                            String value = m.group();
                            int beforeNumber = cursorPosition - value.length();
                            text.insert(beforeNumber, charRespectiveButton[finalI]);
                        }else{
                            text.insert(cursorPosition, charRespectiveButton[finalI]);
                        }
                    }else {
                        text.insert(cursorPosition, charRespectiveButton[finalI]);
                    }
                }
            });
        }


        findViewById(R.id.logo).setVisibility(View.VISIBLE);

        //adiciona evento de click no icone incorporado a direita no xml
        addEventoClickLadoDireito(txtSpeachText);

        perguntas = new ArrayList<>();
        respostas = new ArrayList<>();

        recyclerView = findViewById(R.id.recyclerView);
        //configura o recycler view
        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        //cria a lista personalizada passando os dados para o adapter
        customAdapter = new CustomAdapter(MainActivity.this, perguntas, respostas);
        //atribui a lista com recyclew view para exibir
        recyclerView.setAdapter(customAdapter);

        txtSpeachText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        txtSpeachText.setRawInputType(InputType.TYPE_CLASS_TEXT);
        txtSpeachText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                //se clicar no botao enter do teclado.
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    calcular(false);
                    return true;
                }
                return false;
            }
        });

        txtSpeachText.addTextChangedListener(new TextWatcher() {
            //caso deletar $ e R anteceder apagar junto
            int totalOldChar = 0;
            int totalNewChar = 0;
            char beforeChar;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //caso deletar $ e R anteceder apagar junto
                totalOldChar = txtSpeachText.getText().length();
                if(s.length() > 0) {
                    int index = txtSpeachText.getSelectionStart();
                    if(index > 0) {
                        beforeChar = s.charAt(index - 1);
                    }
                    //Toast.makeText(MainActivity.this, String.valueOf(beforeChar), Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //change icon Send/mic
                temAlgoNoEdiText = s.length() > 0;
                if(temAlgoNoEdiText){
                    txtSpeachText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_send, 0);
                }else{
                    txtSpeachText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_mic, 0);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                //caso deletar $ e R anteceder apagar junto
                int length = s.length();
                if(length > 0) {
                    if (totalOldChar > length) {
                        int index = txtSpeachText.getSelectionStart();
                        if(index > 0) {
                            int beforeIndex = index -1 ;
                            char c = s.charAt(beforeIndex);
                            if ((c == 'R' || c == 'r') && beforeChar == '$') {
                                if (length > 0) {
                                    s.delete(beforeIndex, beforeIndex + 1);
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_principal, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId){
            case  R.id.action_trocarModo:
                View[] mostrar;
                View[] esconder;
                if(txtNumber1.getVisibility() == View.VISIBLE){
                    lblInicial.setText("Fale ou escreva, o que deseja saber?");
                    mostrar = new View[] { txtSpeachText} ;
                    esconder = new View[] {txtNumber1, txtNumber2, btnCalcular} ;
                }else {
                lblInicial.setText("Apenas coloque os números e clique.");
                    mostrar = new View[] {txtNumber1, txtNumber2, btnCalcular} ;
                    esconder = new View[] { txtSpeachText} ;
                }
                crossFadeGONE(mostrar, esconder);
                break;
            case R.id.action_duvida:
                showInformation();
                break;
            case R.id.action_exibir_atalhos:
                item.setChecked(!item.isChecked());
                if(item.isChecked()){
                    for (Button bt:botoesAtalhos) {
                        bt.setVisibility(View.VISIBLE);
                    }
                    //Toast.makeText(this, "Exibir histórico", Toast.LENGTH_SHORT).show();
                }else{
                    for (Button bt:botoesAtalhos) {
                        bt.setVisibility(View.GONE);
                    }
                }

                //implements
                break;
            case R.id.action_escutar_resposta:
                falarResposta = !item.isChecked();
                item.setChecked(falarResposta);
                if(item.isChecked()){
                    Toast.makeText(this, "Escutar resposta.", Toast.LENGTH_SHORT).show();
                }
                //implements
                break;
            case R.id.action_limpar_tudo:
                item.setChecked(!item.isChecked());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /***
     *  Adiciona efeito de exibir e esconder em multiplas views que são trabalhadas em
     *  relativeLayout a sobrepor outras views com visibilidade GONE
     * @param viewShow
     * @param viewHide
     */
    public void crossFadeGONE(final View[] viewShow, View... viewHide) {
            for (int i = 0; i < viewHide.length ; i++) {
                final int pos = i;
                viewHide[i].animate().alpha(0f).setDuration(250).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        viewHide[pos].setVisibility(View.GONE);
                        boolean ultimoHide = viewHide.length - 1 == pos;
                        if(ultimoHide){
                            for (int j = 0; j < viewShow.length; j++){
                                viewShow[j].setAlpha(0f);
                                viewShow[j].setVisibility(View.VISIBLE);
                                viewShow[j].animate()
                                        .alpha(1f)
                                        .setDuration(100)
                                        .setListener(null);
                            }
                        }
                    }
                });

        }
    }

    private void calcular(boolean perguntaFalada){
        Porcentagem porcentagem = new Porcentagem();
        String pergunta = txtSpeachText.getText().toString();
        String resposta = porcentagem.perguntar(pergunta, perguntaFalada);

        perguntas.add(pergunta);
        respostas.add(resposta);
        customAdapter.notifyDataSetChanged();
        recyclerView.smoothScrollToPosition(customAdapter.getItemCount() - 1);

        if(falarResposta){
            falar(resposta);
        };

        txtSpeachText.setText("");
    }

    private void ouvir(){
        Intent speachIntText = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speachIntText.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speachIntText.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
            speachIntText.putExtra(RecognizerIntent.EXTRA_PROMPT, "Pergunte.");
            try {
                startActivityForResult(speachIntText, RECOGNIZER_RESULT);
            }catch (ActivityNotFoundException e){
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
    }

    private void falar(String texto){
        textToSpeech.speak(texto, textToSpeech.QUEUE_ADD, null, "");
    }

    private void iniciarFalador(){
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == textToSpeech.SUCCESS){
                    textToSpeech.setLanguage(Locale.getDefault());
                    textToSpeech.setSpeechRate(1.15f);
                    textToSpeech.setPitch(-2.4f);
                }
            }
        });
    }

    private void showInformation(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater layoutInflater = this.getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.custom_alert_help_dialog, null);

        AlertDialog alertDialog = builder.setView(view)
                .setNegativeButton("Criado por Caio S.", null)
                .setPositiveButton("Voltar", null)
                .create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button button = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                if (button != null) {
                    button.setEnabled(false);
                }

            }
        });
        alertDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == RECOGNIZER_RESULT && resultCode == RESULT_OK){
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            //Resultado de OUVIR
            txtSpeachText.setText(matches.get(0).toString());
            calcular(true);
            //Clica no botao Calcular
            //btnCalcular.performClick();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    @SuppressLint("ClickableViewAccessibility")
    private void addEventoClickLadoDireito(EditText txtComIcone){

        txtComIcone.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                final int DRAWABLE_LEFT = 0;
                final int DRAWABLE_TOP = 1;
                final int DRAWABLE_RIGHT = 2;
                final int DRAWABLE_BOTTOM = 3;

                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (txtComIcone.getRight() - txtComIcone.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        // your action here
                        if(!temAlgoNoEdiText){
                            ouvir();
                        }else{
                            calcular(false);
                        }
                        return true;
                    }
                }
                return false;
            }
        });

    }
}