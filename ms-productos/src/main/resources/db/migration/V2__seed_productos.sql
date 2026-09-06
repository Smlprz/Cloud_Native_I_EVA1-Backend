-- Datos semilla: recrean el catalogo original de la tienda de mascotas
-- (la base Oracle anterior se perdio al vencer la Wallet).

INSERT INTO productos (nombre, descripcion, precio, stock, categoria, imagen_url, id_vendedor) VALUES
('Alimento Perro Adulto Cachupin 25kg', 'Alimento balanceado para perros adultos, saco de 25 kg.', 32990.00, 40, 'Alimento Perros', 'AlimentoPerroAdultoCachupin25kg.webp', 1),
('Alimento Perro Adulto Champion', 'Alimento seco Champion para perros adultos, todas las razas.', 18990.00, 60, 'Alimento Perros', 'AlimentoPerroAdultoChampion.webp', 1),
('Alimento Perro Adulto Champion Dog 3kg', 'Formato 3 kg de alimento Champion Dog para perro adulto.', 6990.00, 80, 'Alimento Perros', 'AlimentoPerroAdultoChampionDog3kg.webp', 1),
('Bolso Mascota Gato Multifuncional', 'Bolso de transporte multifuncional para gatos y perros pequenos.', 24990.00, 15, 'Accesorios', 'BolsoMascotaGatoMultifuncional.webp', 2),
('Casa Perro Talla L', 'Casa plastica para perros de talla grande, resistente a la intemperie.', 45990.00, 8, 'Habitat', 'CasaPerroTallaL.webp', 2),
('Contenedor Comida Mascota 20kg', 'Contenedor hermetico para almacenar hasta 20 kg de alimento.', 19990.00, 20, 'Accesorios', 'ContenedorComidaMascota20kf.webp', 2),
('Rascador Gato 137cm', 'Rascador para gatos de 137 cm con plataformas y sisal natural.', 39990.00, 12, 'Juguetes', 'RascadorGato137cm.webp', 3),
('Salsa Perro 300gr', 'Salsa complemento para alimento de perro, pote de 300 gr.', 2490.00, 100, 'Snacks', 'SalsaPerro300gr.webp', 3);
