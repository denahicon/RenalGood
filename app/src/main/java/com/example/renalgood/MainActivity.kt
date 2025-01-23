package com.example.renalgood

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.renalgood.Nutriologo.NutriologoActivity
import com.example.renalgood.Paciente.PacienteActivity
import com.example.renalgood.admin.AdminActivity
import com.example.renalgood.auth.RecuperarContrasenaActivity
import com.example.renalgood.auth.RegistroNutriologoActivity
import com.example.renalgood.auth.RegistroPacienteActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.AutoCompleteTextView

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    private lateinit var editTextUsuario: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonRecuperar: Button
    private lateinit var spinnerRegistro: AutoCompleteTextView
    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val userCache = mutableMapOf<String, Triple<String, Long, DocumentSnapshot>>()
    private val CACHE_DURATION = 5 * 60 * 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeFirebase()
        initializeViews()
        setupSpinner()
        setupClickListeners()
        checkPreviousSession()
    }

    private fun checkPreviousSession() {
        val prefs = getSharedPreferences("login_prefs", MODE_PRIVATE)
        val shouldRemainLoggedIn = prefs.getBoolean("keep_session", false)

        if (!shouldRemainLoggedIn) {
            mAuth.signOut()
            return
        }

        mAuth.currentUser?.let { user ->
            verificarTipoUsuario(user.uid)
        }
    }

    private fun initializeFirebase() {
        mAuth = FirebaseAuth.getInstance()
        mAuth.setLanguageCode("es")
        db = FirebaseFirestore.getInstance()
    }

    private fun initializeViews() {
        editTextUsuario = findViewById(R.id.usuario)
        editTextPassword = findViewById(R.id.contrasena)
        buttonLogin = findViewById(R.id.iniciar)
        buttonRecuperar = findViewById(R.id.recuperar_contrasena)
        spinnerRegistro = findViewById(R.id.registro_spinner)
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.tipo_usuario,
            android.R.layout.simple_dropdown_item_1line
        )
        spinnerRegistro.setAdapter(adapter)

        spinnerRegistro.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            if (position > 0) {
                val tipoUsuario = parent.getItemAtPosition(position).toString()
                when (tipoUsuario) {
                    "Nutriologo" -> irANutriologoRegistro()
                    "Paciente" -> irAPacienteRegistro()
                }
                spinnerRegistro.setText("", false)
            }
        }
    }

    private fun setupClickListeners() {
        buttonLogin.setOnClickListener { realizarInicioSesion() }
        buttonRecuperar.setOnClickListener { irARecuperarContrasena() }
    }

    private fun realizarInicioSesion() {
        val email = editTextUsuario.text.toString().trim()
        val password = editTextPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                authResult.user?.let { user ->
                    verificarTipoUsuario(user.uid)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al iniciar sesión: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun verificarTipoUsuario(userId: String) {
        // Verificar cache primero
        userCache[userId]?.let { (tipo, timestamp, docSnapshot) ->
            if (System.currentTimeMillis() - timestamp < CACHE_DURATION) {
                when (tipo) {
                    "nutriologo" -> procesarLoginNutriologo(docSnapshot)
                    "paciente" -> irAPaciente()
                    "admin" -> irAPanelAdmin()
                    else -> verificarEnFirestore(userId)
                }
                return
            }
        }

        verificarEnFirestore(userId)
    }

    private fun verificarEnFirestore(userId: String) {
        // Crear batch de consultas
        val nutriologoRef = db.collection("nutriologos").document(userId)
        val pacienteRef = db.collection("patients").document(userId)
        val adminRef = db.collection("admins").document(userId)

        db.runTransaction { transaction ->
            // Primero verificar si es admin
            val adminDoc = transaction.get(adminRef)
            if (adminDoc.exists()) {
                userCache[userId] = Triple("admin", System.currentTimeMillis(), adminDoc)
                runOnUiThread { irAPanelAdmin() }
                return@runTransaction
            }

            // Luego verificar si es nutriólogo
            val nutriDoc = transaction.get(nutriologoRef)
            if (nutriDoc.exists()) {
                userCache[userId] = Triple("nutriologo", System.currentTimeMillis(), nutriDoc)
                runOnUiThread { procesarLoginNutriologo(nutriDoc) }
                return@runTransaction
            }

            // Finalmente verificar si es paciente
            val patientDoc = transaction.get(pacienteRef)
            if (patientDoc.exists()) {
                userCache[userId] = Triple("paciente", System.currentTimeMillis(), patientDoc)
                runOnUiThread { irAPaciente() }
                return@runTransaction
            }

            // Si no existe en ninguna colección
            runOnUiThread {
                Log.e(TAG, "Usuario no encontrado en ninguna colección")
                Toast.makeText(this, "Error: Usuario no encontrado", Toast.LENGTH_SHORT).show()
                mAuth.signOut()
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error en verificación", e)
            Toast.makeText(this, "Error de verificación: ${e.message}", Toast.LENGTH_SHORT).show()
            mAuth.signOut()
        }
    }

    private fun procesarLoginNutriologo(nutriDoc: DocumentSnapshot) {
        val estado = nutriDoc.getString("estado") ?: ""
        val verificado = nutriDoc.getBoolean("verificado") ?: false

        when {
            verificado && (estado == "aprobado" || estado == "activo") -> {
                irANutriologo()
            }
            !verificado -> {
                Toast.makeText(this, "Tu cuenta está pendiente de verificación",
                    Toast.LENGTH_LONG).show()
                mAuth.signOut()
            }
            else -> {
                Toast.makeText(this, "Tu cuenta no está aprobada",
                    Toast.LENGTH_LONG).show()
                mAuth.signOut()
            }
        }
    }

    private fun irAPanelAdmin() {
        val intent = Intent(this, AdminActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun irANutriologo() {
        startActivity(Intent(this, NutriologoActivity::class.java))
        finish()
    }

    private fun irAPaciente() {
        startActivity(Intent(this, PacienteActivity::class.java))
        finish()
    }

    private fun irANutriologoRegistro() {
        startActivity(Intent(this, RegistroNutriologoActivity::class.java))
    }

    private fun irAPacienteRegistro() {
        startActivity(Intent(this, RegistroPacienteActivity::class.java))
    }

    private fun irARecuperarContrasena() {
        startActivity(Intent(this, RecuperarContrasenaActivity::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        userCache.clear()
    }
}