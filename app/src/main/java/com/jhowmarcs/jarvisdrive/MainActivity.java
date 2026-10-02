package com.jhowmarcs.jarvisdrive;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.*;
import android.net.Uri;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.*;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
  static final int SPEECH=7, LOC=8; TextView status,info; EditText cmd; TextToSpeech tts; SharedPreferences p;
  int bg=Color.rgb(5,11,20), card=Color.rgb(12,27,45), cyan=Color.rgb(101,220,255);
  public void onCreate(Bundle b){super.onCreate(b);p=getSharedPreferences("jarvis",0);tts=new TextToSpeech(this,this);ui();refresh();}
  public void onInit(int s){if(s==TextToSpeech.SUCCESS){tts.setLanguage(new Locale("pt","BR"));tts.setSpeechRate(1.03f);}}
  protected void onDestroy(){if(tts!=null)tts.shutdown();super.onDestroy();}
  TextView text(String s,int z){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(Color.WHITE);v.setPadding(18,14,18,14);return v;}
  Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackgroundColor(card);return b;}
  void ui(){getWindow().setStatusBarColor(bg);ScrollView sc=new ScrollView(this);LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(20,20,20,30);r.setBackgroundColor(bg);sc.addView(r);setContentView(sc);
    TextView brand=text("J A R V I S   D R I V E",20);brand.setTextColor(cyan);brand.setGravity(Gravity.CENTER);r.addView(brand);
    TextView orb=text("◉",72);orb.setTextColor(cyan);orb.setGravity(Gravity.CENTER);r.addView(orb);
 status=text("Pronto para dirigir.",18);status.setGravity(Gravity.CENTER);r.addView(status);
    Button mic=button("🎙 FALAR COM JARVIS");mic.setBackgroundColor(cyan);mic.setTextColor(bg);mic.setOnClickListener(v->listen());r.addView(mic);
 cmd=new EditText(this);cmd.setHint("Ex.: gastei 100 reais de combustível");cmd.setTextColor(Color.WHITE);cmd.setHintTextColor(Color.GRAY);r.addView(cmd);
    Button send=button("ENVIAR COMANDO");send.setOnClickListener(v->{String s=cmd.getText().toString();cmd.setText("");handle(s);});r.addView(send);
 info=text("",16);info.setBackgroundColor(card);r.addView(info);
    String[] a={"🧭 NOVA ROTA","🌦 CLIMA","⛽ REGISTRAR GASTO","🧠 MEMORIZAR","🚙 ABRIR WAZE","🗺 GOOGLE MAPS","🎵 SPOTIFY"};
    for(String x:a){Button q=button(x);q.setOnClickListener(v->quick(((Button)v).getText().toString()));r.addView(q);}
  }
  void quick(String s){if(s.contains("ROTA"))route();else if(s.contains("CLIMA"))weather();else if(s.contains("GASTO"))expense();else if(s.contains("MEMORIZAR"))memory();else if(s.contains("WAZE"))open("https://waze.com/ul");else if(s.contains("MAPS"))open("https://www.google.com/maps");else open("https://open.spotify.com");}
  void handle(String s){if(s==null)return;String x=s.toLowerCase(new Locale("pt","BR"));if(x.contains("clima")||x.contains("tempo")){weather();return;}if(x.contains("gastei")){String n=x.replaceAll("[^0-9,.]"," ").trim().split(" ")[0];double v=num( );if(v>0){addExpense(v);say("Registrei "+money(v)+".");return;}}if(x.startsWith("memorize ")||x.startsWith("lembre ")){addMemory(s.substring(s.indexOf(' ')+1));say("Memória guardada.");return;}if(x.contains("quanto")&&x.contains("gastei")){say("Hoje você registrou "+money(total())+".");return;}int k=Math.max(x.indexOf(" para "),x.indexOf(" pra "));if((x.contains("rota")||x.contains("vamos"))&&k>0){go(s.substring(k+6));return;}say("Tente clima, rota, gasto ou memória.");}
  void listen(){try{Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"pt-BR");startActivityForResult(i,SPEECH);}catch(Exception e){say("Reconhecimento de voz indisponível.");}}
  protected void onActivityResult(int q,int r,Intent d){super.onActivityResult(q,r,d);if(q==SPEECH&&r==RESULT_OK&&d!=null){ArrayList&lt;String&gt;a=d.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(a!=null&&!a.isEmpty())handle(a.get(0));}}
  void say(String s){status.setText(s);if(tts!=null)tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"jarvis");}
  void open(String u){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(u)));}catch(Exception e){say("Não consegui abrir.");}}
  void route(){EditText e=new EditText(this);e.setHint("Destino");new AlertDialog.Builder(this).setTitle("Nova rota").setView(e).setNegativeButton("Cancelar",null).setPositiveButton("Waze",(d,w)->go(e.getText().toString())).setNeutralButton("Maps",(d,w)->open("https://www.google.com/maps/dir/?api=1&destination="+Uri.encode(e.getText().toString()))).show();}
  void go(String d){if(!d.trim().isEmpty()){p.edit().putInt("trips",p.getInt("trips",0)+1).apply();refresh();open("https://waze.com/ul?q="+Uri.encode(d)+"&navigate=yes");}}
  void expense(){EditText e=new EditText(this);e.setHint("Valor");new AlertDialog.Builder(this).setTitle("Registrar gasto").setView(e).setNegativeButton("Cancelar",null).setPositiveButton("Salvar",(d,w)->{double v=num(e.getText().toString());if(v>0){addExpense(v);say("Gasto salvo.");}}).show();}
  void addExpense(double v){try{JSONArray a=new JSONArray(p.getString("expenses","[]"));JSONObject o=new JSONObject();o.put("time",System.currentTimeMillis());o.put("value",v);a.put(o);p.edit().putString("expenses",a.toString()).apply();refresh();}catch(Exception ignored){}}
  double total(){double t=0;try{JSONArray a=new JSONArray(p.getString("expenses","[]"));Calendar n=Calendar.getInstance();for(int i=0;i&lt;a.length();i++){JSONObject o=a.getJSONObject(i);Calendar c=Calendar.getInstance();c.setTimeInMillis(o.getLong("time"));if(c.get(Calendar.DAY_OF_YEAR)==n.get(Calendar.DAY_OF_YEAR)&&c.get(Calendar.YEAR)==n.get(Calendar.YEAR))t+=o.getDouble("value");}}catch(Exception ignored){}return t;}
  void memory(){EditText e=new EditText(this);e.setHint("O que devo lembrar?");new AlertDialog.Builder(this).setTitle("Memória").setView(e).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",(d,w)->{addMemory(e.getText().toString());say("Memória guardada.");}).show();}
  void addMemory(String s){if(s.trim().isEmpty())return;try{JSONArray a=new JSONArray(p.getString("mem","[]"));a.put(s.trim());p.edit().putString("mem",a.toString()).apply();refresh();}catch(Exception ignored){}}
  int mems(){try{return new JSONArray(p.getString("mem","[]")).length();}catch(Exception e){return 0;}}
  void refresh(){info.setText("GASTOS HOJE  "+money(total())+"\nVIAGENS  "+p.getInt("trips",0)+"    MEMÓRIAS  "+mems());}
  double num(String s){try{return Double.parseDouble(s.replace(',','.').trim());}catch(Exception e){return 0;}}
  String money(double v){return NumberFormat.getCurrencyInstance(new Locale("pt","BR")).format(v);}
  void weather(){if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION},LOC);return;}try{LocationManager m=(LocationManager)getSystemService(LOCATION_SERVICE);Location l=m.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);if(l==null)l=m.getLastKnownLocation(LocationManager.GPS_PROVIDER);if(l==null){say("Ative a localização e tente novamente.");return;}fetch(l.getLatitude(),l.getLongitude());}catch(Exception e){say("Não consegui obter sua localização.");}}
  public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==LOC&&g.length&gt;0&&g[0]==PackageManager.PERMISSION_GRANTED)weather();}
  void fetch(double a,double b){say("Consultando o clima.");new Thread(()->{try{URL u=new URL("https://api.open-meteo.com/v1/forecast?latitude="+a+"&longitude="+b+"&current=temperature_2m,apparent_temperature&timezone=auto");HttpURLConnection c=(HttpURLConnection)u.openConnection();BufferedReader br=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8));StringBuilder z=new StringBuilder();String s;while((s=br.readLine())!=null)z.append(s);br.close();JSONObject o=new JSONObject(z.toString()).getJSONObject("current");long t=Math.round(o.getDouble("temperature_2m")),f=Math.round(o.getDouble("apparent_temperature"));runOnUiThread(()->say("Agora está "+t+" graus, sensação de "+f+" graus."));}catch(Exception e){runOnUiThread(()->say("Não consegui consultar o clima."));}}).start();}
}
