-- Conturi de test. Parole: admin1234, ana12345, mihai1234 (hash-uri BCrypt)
INSERT INTO utilizator (email, parola_hash, nume, rol) VALUES
    ('admin@chatapp.ro',
     '{bcrypt}$2a$10$cEWKFDReaaAJjCtfzeHG/uY0FZj73/yhBpCXCHpO90zxlaJHc77fa',
     'Administrator', 'ADMIN'),
    ('ana@chatapp.ro',
     '{bcrypt}$2a$10$U9LNqO.fuNarO49ruU6jvOcHolA6TIpmUSYU.7Q3ZBinr6TtP5yRG',
     'Ana Popescu', 'USER'),
    ('mihai@chatapp.ro',
     '{bcrypt}$2a$10$YQVqOScj9RbrVhmMZDsaxuKq8fTZ1.Yi0hkMjF2qDgxuTohpI6MNq',
     'Mihai Ionescu', 'USER');
