package com.example.renalgood

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.renalgood.Nutriologo.NutriologoActivity
import com.example.renalgood.Paciente.PacienteActivity
import com.example.renalgood.auth.AdminLoginActivity
import com.example.renalgood.auth.RecuperarContrasenaActivity
import com.example.renalgood.auth.RegistroNutriologoActivity
import com.example.renalgood.auth.RegistroPacienteActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    private lateinit var editTextUsuario: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonRecuperar: Button
    private lateinit var spinnerRegistro: Spinner
    private lateinit var buttonAdmin: Button
    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var eyeIcon: ImageView
    private var isPasswordVisible = false
    private val userCache = mutableMapOf<String, Pair<String, Long>>()
    private val CACHE_DURATION = 5 * 60 * 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeFirebase()
        initializeViews()
        setupSpinner()
        setupClickListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        userCache.clear()
    }

    private fun initializeFirebase() {
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    private fun initializeViews() {
        editTextUsuario = findViewById(R.id.usuario)
        editTextPassword = findViewById(R.id.contrasena)
        buttonLogin = findViewById(R.id.iniciar)
        buttonRecuperar = findViewById(R.id.recuperar_contrasena)
        spinnerRegistro = findViewById(R.id.registro_spinner)
        buttonAdmin = findViewById(R.id.btnAdminLogin)
        eyeIcon = findViewById(R.id.ojo_contrasena)
        eyeIcon.setOnClickListener { togglePasswordVisibility() }
        eyeIcon = findViewById(R.id.ojo_contrasena)
    }

    private fun setupSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.tipo_usuario,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerRegistro.adapter = adapter
        }

        spinnerRegistro.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {
                if (position > 0) { // Ignorar la primera opción "Registrarse como"
                    val tipoUsuario = parentView.getItemAtPosition(position).toString()
                    when (tipoUsuario) {
                        "Nutriologo" -> irANutriologoRegistro()
                        "Paciente" -> irAPacienteRegistro()
                    }
                    spinnerRegistro.setSelection(0) // Resetear el spinner
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {}
        }
    }

    private fun setupClickListeners() {
        buttonLogin.setOnClickListener { realizarInicioSesion() }
        buttonRecuperar.setOnClickListener { irARecuperarContrasena() }
        buttonAdmin.setOnClickListener {
            startActivity(Intent(this, AdminLoginActivity::class.java))
        }
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

    private fun verificarAdmin(uid: String?) {
        if (uid == null) {
            Toast.makeText(this, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("admins")
            .document(uid)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Es admin, ir a panel de admin
                    irAPanelAdmin()
                } else {
                    // No es admin
                    Toast.makeText(this, "No tienes permisos de administrador", Toast.LENGTH_SHORT).show()
                    mAuth.signOut()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al verificar permisos: ${e.message}", Toast.LENGTH_SHORT).show()
                mAuth.signOut()
            }
    }

    private fun verificarTipoUsuario(userId: String) {
        // Verificar cache primero
        userCache[userId]?.let { (tipo, timestamp) ->
            if (System.currentTimeMillis() - timestamp < CACHE_DURATION) {
                when (tipo) {
                    "nutriologo" -> irANutriologo()
                    "paciente" -> irAPaciente()
                    "admin" -> irAPanelAdmin()
                    else -> verificarEnFirestore(userId)
                }
                return
            }
        }

        verificarEnFirestore(userId)
    }

    private fun verificarOtrosTiposUsuario(userId: String) {
        // Verificar si es paciente o admin como lo tenías antes
        db.collection("patients")
            .document(userId)
            .get()
            .addOnSuccessListener { patientDoc ->
                if (patientDoc.exists()) {
                    irAPaciente()
                } else {
                    db.collection("admins")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { adminDoc ->
                            if (adminDoc.exists()) {
                                irAPanelAdmin()
                            } else {
                                Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_LONG).show()
                                mAuth.signOut()
                            }
                        }
                }
            }
    }

    private fun verificarEnFirestore(userId: String) {
        // Crear batch de consultas
        val nutriologoRef = db.collection("nutriologos").document(userId)
        val pacienteRef = db.collection("patients").document(userId)
        val adminRef = db.collection("admins").document(userId)

        db.runTransaction { transaction ->
            val nutriDoc = transaction.get(nutriologoRef)

            if (nutriDoc.exists()) {
                val estado = nutriDoc.getString("estado") ?: ""
                val verificado = nutriDoc.getBoolean("verificado") ?: false

                when {
                    verificado && (estado == "aprobado" || estado == "activo") -> {
                        // Guardar en cache
                        userCache[userId] = "nutriologo" to System.currentTimeMillis()
                        runOnUiThread { irANutriologo() }
                    }
                    !verificado -> {
                        runOnUiThread {
                            Toast.makeText(this, "Tu cuenta está pendiente de verificación",
                                Toast.LENGTH_LONG).show()
                            mAuth.signOut()
                        }
                    }
                    else -> {
                        runOnUiThread {
                            Toast.makeText(this, "Tu cuenta no está aprobada",
                                Toast.LENGTH_LONG).show()
                            mAuth.signOut()
                        }
                    }
                }
                return@runTransaction
            }

            // Si no es nutriólogo, verificar paciente
            val patientDoc = transaction.get(pacienteRef)
            if (patientDoc.exists()) {
                userCache[userId] = "paciente" to System.currentTimeMillis()
                runOnUiThread { irAPaciente() }
                return@runTransaction
            }

            // Si no es paciente, verificar admin
            val adminDoc = transaction.get(adminRef)
            if (adminDoc.exists()) {
                userCache[userId] = "admin" to System.currentTimeMillis()
                runOnUiThread { irAPanelAdmin() }
                return@runTransaction
            }

            // Si no existe en ninguna colección
            runOnUiThread {
                Log.e(TAG, "Usuario no encontrado en ninguna colección")
                Toast.makeText(this, "Error: Usuario no encontrado",
                    Toast.LENGTH_SHORT).show()
                mAuth.signOut()
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error en verificación", e)
            Toast.makeText(this, "Error de verificación: ${e.message}",
                Toast.LENGTH_SHORT).show()
            mAuth.signOut()
        }
    }

    private fun actualizarTipoUsuario(userId: String, tipoUsuario: String) {
        val userDoc = hashMapOf(
            "tipoUsuario" to tipoUsuario,
            "email" to mAuth.currentUser?.email,
            "lastLogin" to System.currentTimeMillis()
        )

        db.collection("users").document(userId)
            .set(userDoc)
            .addOnSuccessListener {
                Log.d(TAG, "Tipo de usuario actualizado a: $tipoUsuario")
                when (tipoUsuario) {
                    "Nutriologo" -> irANutriologo()
                    "Paciente" -> irAPaciente()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al actualizar tipo de usuario", e)
                Toast.makeText(this, "Error al actualizar tipo de usuario: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun verificarEstadoNutriologo(uid: String) {
        db.collection("nutriologos")
            .document(uid)
            .get()
            .addOnSuccessListener { documentSnapshot: DocumentSnapshot ->
                if (documentSnapshot.exists()) {
                    val verificado = documentSnapshot.getBoolean("verificado")
                    if (verificado != null && verificado) {
                        actualizarTipoUsuario(uid, "Nutriologo")
                    } else {
                        Toast.makeText(
                            this, "Tu cuenta aún no ha sido verificada",
                            Toast.LENGTH_LONG
                        ).show()
                        FirebaseAuth.getInstance().signOut()
                    }
                } else {
                    Toast.makeText(
                        this, "No se encontró la información del nutriólogo",
                        Toast.LENGTH_LONG
                    ).show()
                    FirebaseAuth.getInstance().signOut()
                }
            }
    }

    private fun limpiarCacheExpirado() {
        val currentTime = System.currentTimeMillis()
        userCache.entries.removeAll { (_, value) ->
            currentTime - value.second > CACHE_DURATION
        }
    }

    private fun irAPanelAdmin() {
        val intent = Intent(this, AdminLoginActivity::class.java)
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

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible

        if (isPasswordVisible) {
            editTextPassword.transformationMethod = null
            eyeIcon.setImageResource(R.drawable.ic_eye_open)
        } else {
            editTextPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            eyeIcon.setImageResource(R.drawable.ic_eye_closed)
        }
        editTextPassword.setSelection(editTextPassword.text.length)
    }

    private fun validateFields(): Boolean {
        val password = editTextPassword.text.toString().trim()

        if (password.isEmpty()) {
            editTextPassword.error = "Por favor ingrese su contraseña"
            return false
        }

        return true
    }
}