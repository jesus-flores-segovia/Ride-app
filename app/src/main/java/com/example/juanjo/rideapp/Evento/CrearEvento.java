package com.example.juanjo.rideapp.Evento;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.juanjo.rideapp.DTO.EventoDTO;
import com.example.juanjo.rideapp.DTO.Ruta_infoDTO;
import com.example.juanjo.rideapp.Login.Login;
import com.example.juanjo.rideapp.R;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;

/**
 * Clase encargada de gestionar la ventana de creacioón de un Evento
 * @author RideApp
 */
public class CrearEvento extends AppCompatActivity {
    public static final String URL = "http://rideapp2.somee.com/WebService.asmx";
    public static final String METHOD_NAME_CREAREVENTOS = "nuevoEvento";
    public static final String SOAP_ACTION_CREAREVENTOS = "http://tempuri.org/nuevoEvento";
    public static final String METHOD_NAME_RUTAS = "listaRuta_info_byIdUsuario";
    public static final String SOAP_ACTION_RUTAS = "http://tempuri.org/listaRuta_info_byIdUsuario";
    public static final String NAMESPACE = "http://tempuri.org/";

    private Context mContext;
    private EventoDTO eventoDTO;
    private LinkedList<Ruta_infoDTO> info_rutas;
    private ArrayList<String> listaRutas;
    private String fechaEvento;

    private CalendarView calendario;
    private EditText descripcion;
    private Spinner selectorRutas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_crear_evento);
        info_rutas = new LinkedList<Ruta_infoDTO>();
        listaRutas = new ArrayList<String>();
        mContext = this;
        eventoDTO = null;
        calendario = findViewById(R.id.CrearEvento_calendarView);
        Long milisegundos = calendario.getDate();
        SimpleDateFormat format =
                new SimpleDateFormat("MM-dd-yyyy");
        fechaEvento = format.format(milisegundos);
        descripcion = findViewById(R.id.CrearEvento_descripcion);
        selectorRutas = findViewById(R.id.CrearEvento_spinner_ruta);
        cargarRutasLista();
        calendario.setOnDateChangeListener(new CalendarView.OnDateChangeListener(){
            @Override
            public void onSelectedDayChange(CalendarView view, int year,
                                            int month, int dayOfMonth) {
                fechaEvento = month+1+"-"+dayOfMonth+"-"+year;
            }

        });
    }

    /**
     * Se genera un Handler encargado de ejecutar la consulta para obtener las rutas del usuario logeado,
     * carga los datos recibidos en un arraylist concatenando el ID de Ruta, los caracteres ".-" y el título de la ruta.
     * Una vez obtenido los datos se crea el adaptador y se carga el spinner de selección de ruta.
     */
    private void cargarRutasLista(){
        Handler uiHandlerLisaRutas = new Handler(this.getMainLooper());
        uiHandlerLisaRutas.post(new Runnable() {
            @Override
            public void run() {
                ObtenerInfoRutas selectRutas = new ObtenerInfoRutas(mContext);
                try {
                    selectRutas.execute(Login.getUsuari().getIdUsuario()).get();
                    if(info_rutas.size()>0 && info_rutas!=null){
                        for(Ruta_infoDTO ruta : info_rutas){
                            listaRutas.add(ruta.getIdRuta()+".- "+ruta.getTitulo());
                        }
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_item, listaRutas);
                    selectorRutas.setAdapter(adapter);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Se abre una instancia de EventoDTO a la cual se le cargarán los datos.
     * El ID de usuario se obtiene del método estático getUsuari de la clase Login.
     * Se obtiene la fecha del CalendarView, para el ID de la ruta se ejecuta el método split sobre el item seleccionado del spinner.
     * Y los comentarios del evento se obtienen del widget edittext.
     * Una vez preparado el objeto a insertar a la BD, se genera un handler para ejecutar la consulta de InsertarEvento y finaliza la activdad.
     * @param view Botón guardarRuta con este método como onClick.
     */
    public void guardarRuta(View view){
        eventoDTO = new EventoDTO();
        eventoDTO.setAdmin(Login.getUsuari().getIdUsuario());
        eventoDTO.setFecha(fechaEvento);
        String[] idRutaString = selectorRutas.getSelectedItem().toString().split(".-");
        eventoDTO.setRuta(Integer.valueOf(idRutaString[0]));
        eventoDTO.setComentario(descripcion.getText().toString());
        Handler uiHandlerInsertarEvento = new Handler(this.getMainLooper());
        uiHandlerInsertarEvento.post(new Runnable() {
            @Override
            public void run() {
                InsertarEvento insertarEvento = new InsertarEvento(mContext);
                insertarEvento.execute();
            }
        });
        finish();
    }

    /**
     * Finaliza la actividad sin hacer el insert a la BD.
     * @param view Botón de cancelar ruta con este método como onClick.
     */
    public void cancelarGuardar(View view){
        this.finish();
    }

    private class InsertarEvento extends AsyncTask<Integer,Void,Integer> {

        private Context context;

        public InsertarEvento(Context context) {
            this.context = context;
        }

        protected Integer doInBackground(Integer... params) {

            Integer result = 0;

            PropertyInfo pi = new PropertyInfo();
            pi.setName("evento");
            pi.setValue(eventoDTO);
            pi.setType(eventoDTO.getClass());

            SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME_CREAREVENTOS);
            request.addProperty(pi);
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.dotNet = true;
            envelope.setOutputSoapObject(request);
            envelope.addMapping(NAMESPACE, "EventoDTO", eventoDTO.getClass());

            HttpTransportSE transporte = new HttpTransportSE(URL);

            try {
                transporte.call(SOAP_ACTION_CREAREVENTOS,envelope);
                SoapPrimitive resSoap = (SoapPrimitive)envelope.getResponse();
                result = Integer.parseInt(resSoap.toString());
            } catch (IOException | XmlPullParserException e) {
                e.printStackTrace();
            }

            return result;

        }
        protected void onPostExecute(Integer result) {
            if(result!= 0){
                Toast.makeText(this.context, "Evento creado! :D", Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(this.context, "No se ha podido crear el evento", Toast.LENGTH_LONG).show();
            }
        }
    }


    private class ObtenerInfoRutas extends AsyncTask<Integer,Void,Boolean> {

        private Context context;

        public ObtenerInfoRutas(Context context) {
            this.context = context;
        }

        protected Boolean doInBackground(Integer... params) {

            Boolean result = true;

            SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME_RUTAS);
            request.addProperty("idUsuario", params[0]);
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.dotNet = true;
            envelope.setOutputSoapObject(request);

            HttpTransportSE transporte = new HttpTransportSE(URL);

            try {
                transporte.call(SOAP_ACTION_RUTAS, envelope);
                SoapObject resSoap = (SoapObject) envelope.getResponse();

                for (int i = 0; i < resSoap.getPropertyCount(); i++) {
                    SoapObject iu = (SoapObject) resSoap.getProperty(i);

                    Ruta_infoDTO ruta_info = new Ruta_infoDTO(Integer.valueOf(iu.getPropertyAsString(0)),
                            iu.getPropertyAsString(1), iu.getPropertyAsString(2), iu.getPropertyAsString(3),
                            Integer.valueOf(iu.getPropertyAsString(4)), Integer.valueOf(iu.getPropertyAsString(5)),
                            Integer.valueOf(iu.getPropertyAsString(6)), iu.getPropertyAsString(7));

                    info_rutas.add(ruta_info);
                }

            } catch (Exception e) {
                result = false;
                e.printStackTrace();
            }


            return result;
        }

        protected void onPostExecute(Boolean result) {
            if (result) {

            } else {

            }
        }
    }
}
