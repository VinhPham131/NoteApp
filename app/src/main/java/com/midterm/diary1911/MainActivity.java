package com.midterm.diary1911;

import static android.app.ProgressDialog.show;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.midterm.diary1911.model.Post;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private DatabaseReference myRefRoot;
    private FirebaseFirestore firestore;
    private RecyclerView recyclerView;
    private FloatingActionButton btnAdd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.id_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Vinh's note");

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        firestore = FirebaseFirestore.getInstance();
        myRef = database.getReference().child("posts");

        btnAdd = findViewById(R.id.btn_add);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNote();
            }
        });

        recyclerView = findViewById(R.id.rv_note);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

    }
    public void addNote() {
        //add note layout
        AlertDialog.Builder mDigalog = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.add_note, null);
        mDigalog.setView(view);

        AlertDialog dialog = mDigalog.create();
        dialog.setCancelable(true);

        //Click save to save note
        Button save = view.findViewById(R.id.btn_save);
        EditText editTitle = view.findViewById(R.id.et_title);
        EditText editContent = view.findViewById(R.id.et_content);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = myRef.push().getKey();
                String title = editTitle.getText().toString();
                String content = editContent.getText().toString();

                myRef.child(id).setValue(new Post(id, title, content, getRandomColor()))
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(MainActivity.this, "Add note success", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(MainActivity.this, "Add note failed", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                dialog.dismiss();
            }
        });
        dialog.show();
    }


    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        FirebaseRecyclerOptions<Post> options =
                new FirebaseRecyclerOptions.Builder<Post>()
                        .setQuery(myRef, Post.class)
                        .build();
        FirebaseRecyclerAdapter adapter = new FirebaseRecyclerAdapter<Post, PostHolder>(options) {
            @Override
            public PostHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                // Create a new instance of the ViewHolder, in this case we are using a custom
                // layout called R.layout.message for each item
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.note_items, parent, false);

                return new PostHolder(view);
            }

            @Override
            protected void onBindViewHolder(PostHolder holder, int position, Post model) {
                holder.title.setText(model.getTitle());
                holder.content.setText(model.getContent());
                holder.layoutNote.setBackgroundColor(Color.parseColor(model.getColor()));

                ImageView ivMore = holder.itemView.findViewById(R.id.iv_more);
                ivMore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                        popupMenu.setGravity(Gravity.END);
                        popupMenu.getMenu().add("Edit").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(@NonNull MenuItem item) {
                                AlertDialog.Builder mDialog = new AlertDialog.Builder(v.getContext());
                                LayoutInflater inflater = LayoutInflater.from(v.getContext());
                                View view = inflater.inflate(R.layout.add_note, null);
                                mDialog.setView(view);

                                AlertDialog dialog = mDialog.create();
                                dialog.setCancelable(true);

                                // Pre-fill the input fields with current note details
                                EditText editTitle = view.findViewById(R.id.et_title);
                                EditText editContent = view.findViewById(R.id.et_content);
                                editTitle.setText(model.getTitle());
                                editContent.setText(model.getContent());

                                Button save = view.findViewById(R.id.btn_save);
                                save.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        // Update the note in Firebase database
                                        String title = editTitle.getText().toString();
                                        String content = editContent.getText().toString();

                                        myRef.child(model.getId()).setValue(new Post(model.getId(), title, content, model.getColor()));
                                        dialog.dismiss();
                                    }
                                });
                                dialog.show();
                                return false;
                            }
                        });
                        popupMenu.getMenu().add("Delete").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(@NonNull MenuItem item) {
                                myRef.child(model.getId()).removeValue();
                                return false;
                            }
                        });
                        popupMenu.show();
                    }
                });
            }
        };
        recyclerView.setAdapter(adapter);
        adapter.startListening();

    }
    public static class PostHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public TextView content;
        public LinearLayout layoutNote;

        public PostHolder(View itemView) {
            super(itemView);
            layoutNote = itemView.findViewById(R.id.ly_note);
            title = itemView.findViewById(R.id.tv_title);
            content = itemView.findViewById(R.id.tv_content);
        }
    }
    private void login(String email, String pass) {
        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            Log.d("Debug", "Login Success");
                        }else {
                            Log.d("Debug", "Login Failed");
                        }
                    }
                });
    }
    private void createNewUser(String newUserEmail, String newUserPass) {
        mAuth.createUserWithEmailAndPassword(newUserEmail, newUserPass)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            Log.d("Debug", "Create new user success");
                        }else {
                            Log.d("Debug", "Create new user failed");
                        }
                    }
                });
    }
    private void resetPass(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d("Debug", "Reset pass success");
                        } else {
                            Log.d("Debug", "Reset pass failed");
                        }
                    }
                });
    }
    private void signOut() {
        mAuth.signOut();
    }
    private void postDataToRealTimeDB(String data) {

        myRef.setValue(data)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d("Debug", "Post data success");
                        } else {
                            Log.d("Debug", "Post data failed");
                        }
                    }
                });
    }
    private void readDataFromRealTimeDB() {
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String value = snapshot.getValue(String.class);
                Log.d("Debug", "Value is: " + value);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("Debug", "Failed to read value", error.toException());
            }
        });
    }
    private void postDataToFireStore() {
        Map<String, Object> user = new HashMap<>();
        user.put("first", "Ada");
        user.put("last", "Lovelace");
        user.put("born", 1815);

        // Add a new document with a generated ID
        firestore.collection("users")
                .add(user)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d("Debug", "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Debug", "Error adding document", e);
                    }
                });
    }
    public void addPostData(Post data) {
        myRefRoot = database.getReference();
        myRefRoot.child("posts").push().setValue(data)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d("Debug", "Post data success");
                        } else {
                            Log.d("Debug", "Post data failed");
                        }
                    }
                });
    }
    //Set up random color for each note
    private String getRandomColor() {
       ArrayList<String> colors = new ArrayList<>();
       colors.add("#ffd8f9");
       colors.add("#999999");
       colors.add("#c6f7c2");
       colors.add("#faebd7");
       colors.add("#c6e2ff");
       Random random = new Random();
       return colors.get(random.nextInt(colors.size()));
    }
    //Set up menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.mi_logout) {
            mAuth.signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}