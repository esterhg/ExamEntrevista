package com.example.examentrevista;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Button guardar,lista;
    private String audioUrl;
    ImageButton reproducir,grabar;
    private EditText periodista,descripcion,fecha;

    private MaterialCardView cardView;
    private Uri ImageUri;
    private Bitmap bitmap;
    private FirebaseStorage storage;
    private FirebaseFirestore firestore;
    private StorageReference mStorage;
    private FirebaseAuth mAuth;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    private String PhotUrl;
    private  String currentUserId;
    ImageView img;
    private int currentId = 0;
    static final int PETICION_ACCESO_PERMISOS = 100;
    ProgressDialog progressDialog;

    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private StorageReference audioStorageRef;
    private String audioFilePath; // Ruta de almacenamiento del archivo de audio
    private MediaPlayer mediaPlayer;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        img = findViewById(R.id.imageView);
        periodista = findViewById(R.id.txtperiodista);
        descripcion = findViewById(R.id.txtdescripcion);
        fecha = findViewById(R.id.txtfecha);

        guardar = (Button) findViewById(R.id.btnActualizar);
        lista = (Button) findViewById(R.id.btnlista);

        reproducir = (ImageButton) findViewById(R.id.btnreproducir);
        grabar = (ImageButton) findViewById(R.id.btngrabar);

        firestore=FirebaseFirestore.getInstance();
        storage=FirebaseStorage.getInstance();
        mStorage=storage.getReference();
        mAuth=FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        lista = (Button) findViewById(R.id.btnlista);
        lista.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), lista.class);
                startActivity(intent);
            }
        });
        guardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadImage();
                inicializarFirebase();
            }
        });
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                permisos();
            }
        });

        audioStorageRef = FirebaseStorage.getInstance().getReference().child("audio");
        grabar = findViewById(R.id.btngrabar);

        grabar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isRecording) {
                    stopRecording();
                } else {
                    startRecording();
                }
            }
        });

        reproducir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reproducirAudio();
            }
        });





    }

    private void permisosAudio(){
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, PETICION_ACCESO_PERMISOS);
        else {
            Obtener();
        }


    }
    private void inicializarFirebase(){
        FirebaseApp.initializeApp(this);
        firebaseDatabase= FirebaseDatabase.getInstance();
        databaseReference=firebaseDatabase.getReference();
    }
    private void permisos() {
        if(Build.VERSION.SDK_INT>Build.VERSION_CODES.M){
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, PETICION_ACCESO_PERMISOS);
            } else {
                Obtener();
            }
        }else {
            Obtener();
        }
    }
    private void Obtener() {

        Intent intent =new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        launcher.launch(intent);
    }
    ActivityResultLauncher<Intent> launcher
            =registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == Activity.RESULT_OK){
                    Intent data = result.getData();
                    ImageUri=data.getData();
                    try {

                        bitmap = MediaStore.Images.Media.getBitmap(
                                getContentResolver(),
                                ImageUri
                        );
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                }
                if ( ImageUri!=null){
                    img.setImageBitmap(bitmap);

                }
            }
    );

    private void uploadImage() {
        if (ImageUri != null) {
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setTitle("Cargando imagen...");
            progressDialog.show();
            String imageName = UUID.randomUUID().toString();
            StorageReference filePath = mStorage.child("images").child(imageName);

            UploadTask uploadTask = filePath.putFile(ImageUri);
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return filePath.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        currentId++;
                        PhotUrl = task.getResult().toString();
                        Toast.makeText(getApplicationContext(), "URL de imagen: " + PhotUrl, Toast.LENGTH_LONG).show();
                        uploadInfo();

                    } else {
                        Toast.makeText(getApplicationContext(), "Error al obtener la URL de la imagen", Toast.LENGTH_SHORT).show();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Error al subir la imagen", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(getApplicationContext(), "Selecciona una imagen primero", Toast.LENGTH_SHORT).show();
        }
    }
    private void uploadInfo() {
        String perio = periodista.getText().toString().trim();
        String descrip = descripcion.getText().toString().trim();
        String fech = fecha.getText().toString().trim();

        if (TextUtils.isEmpty(perio) || TextUtils.isEmpty(descrip)  || TextUtils.isEmpty(fech)) {
            Toast.makeText(getApplicationContext(), "Por Favor rellene todos los datos", Toast.LENGTH_LONG).show();
        } else {
            Entrevista p = new Entrevista();
            p.setId(String.valueOf(currentId));
            p.setPeriodista(perio);
            p.setDescripcion(descrip);
            p.setFecha(fech);
            p.setImg(PhotUrl);
            p.setAudio(audioUrl);
            databaseReference.child("Entrevista").child(p.getId()).setValue(p);
            Toast.makeText(getApplicationContext(), "Agregado", Toast.LENGTH_LONG).show();
        }

    }

    private void startRecording() {
        // Verificar y solicitar permisos si es necesario
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, PETICION_ACCESO_PERMISOS);
            return;
        }


        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // Cambio de formato
        audioFilePath = getExternalCacheDir().getAbsolutePath() + "/audio_record.mp3"; // Cambio de extensión
        mediaRecorder.setOutputFile(audioFilePath);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            grabar.setImageResource(R.drawable.grabar); // Cambiar la imagen del botón
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;

            Uri audioUri = Uri.fromFile(new File(audioFilePath));

            StorageReference audioFileRef = audioStorageRef.child("audio_" + UUID.randomUUID().toString() + ".3gp");

            // Configurar el tipo MIME del archivo a audio/3gpp
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType("audio/3gpp")
                    .build();

            audioFileRef.putFile(audioUri, metadata)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // Obtener la URL de descarga del archivo de audio subido
                            audioFileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {


                                @Override
                                public void onSuccess(Uri downloadUri) {
                                    audioUrl = downloadUri.toString();

                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Manejar el fallo de la subida
                            // ...
                        }
                    });
        }
    }


    private void reproducirAudio() {
        if (!TextUtils.isEmpty(audioFilePath)) {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            }

            if (!mediaPlayer.isPlaying()) {
                try {
                    mediaPlayer.setDataSource(audioFilePath);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    reproducir.setImageResource(R.drawable.reproducir); // Cambiar la imagen del botón a pausa
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                mediaPlayer.pause();
                reproducir.setImageResource(R.drawable.reproducir); // Cambiar la imagen del botón a reproducir
            }
        } else {
            // El archivo de audio no existe o la ruta está vacía
            Toast.makeText(this, "No se encontró el archivo de audio", Toast.LENGTH_SHORT).show();
        }
    }
}