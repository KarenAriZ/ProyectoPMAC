package com.karenarias.proyectopmac.data.model

data class RecargaDto(
    val id: String,
    val numeroTarjeta: String,
    val monto: String,
    val metodoPago: String,
    val fecha: String,
    val estado: String
)