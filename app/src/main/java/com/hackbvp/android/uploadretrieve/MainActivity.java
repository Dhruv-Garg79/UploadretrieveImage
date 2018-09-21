package com.hackbvp.android.uploadretrieve;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;


public class MainActivity extends AppCompatActivity {

    EditText editText;
    ImageView imageView;
    ProgressDialog progress;
    public static final int PICK_IMAGE = 1;

    private Uri imageUri;
    private StorageReference storageReference;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button upload = findViewById(R.id.upload);
        Button select = findViewById(R.id.choose);
        editText = findViewById(R.id.editText);
        imageView = findViewById(R.id.imageView);

        storageReference = FirebaseStorage.getInstance().getReference("uploadsImage");
        databaseReference = FirebaseDatabase.getInstance().getReference("uploadsImage");

        progress = new ProgressDialog(this);

        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent , PICK_IMAGE);
            }
        });

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadFileToFirebase();
            }
        });
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    private void uploadFileToFirebase(){
        if (imageUri != null){

            progress.setMessage("Uploading Images...");
            progress.show();

            final StorageReference fileReference = storageReference.child(System.currentTimeMillis()
                    + "." + getFileExtension(imageUri));

            StorageTask<UploadTask.TaskSnapshot> uploadTask = fileReference.putFile(imageUri);

            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        if (task.getException() != null)
                        throw  task.getException();
                    }

                    return fileReference.getDownloadUrl();
                }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        progress.dismiss();
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();
                            Person person = new Person(editText.getText().toString().trim(), downloadUri.toString() );

                            String uploadId = databaseReference.push().getKey();
                            assert uploadId != null;
                            databaseReference.child(uploadId).setValue(person);

                            Toast.makeText(MainActivity.this, "Upload Successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MainActivity.this, RetriveActivity.class));

                        } else {
                            Toast.makeText(MainActivity.this, "Upload Failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        } else {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK && requestCode == PICK_IMAGE && data != null){
            imageUri = data.getData();
            Picasso.get().load(imageUri).into(imageView);
        }
    }
}