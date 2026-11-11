/* logico: */

CREATE TABLE Aluno (
    id_aluno INTEGER PRIMARY KEY,
    email VARCHAR(120) UNIQUE
);

CREATE TABLE Curso (
    id_curso INTEGER PRIMARY KEY,
    nome VARCHAR(80)
);

CREATE TABLE Matricula (
    id_matricula INTEGER PRIMARY KEY,
    id_aluno INTEGER,
    id_curso INTEGER
);
 
ALTER TABLE Matricula ADD CONSTRAINT FK_Matricula_2
    FOREIGN KEY (id_aluno)
    REFERENCES Aluno (id_aluno);
 
ALTER TABLE Matricula ADD CONSTRAINT FK_Matricula_3
    FOREIGN KEY (id_curso)
    REFERENCES Curso (id_curso);
=====
/* Aula BD: */

CREATE TABLE esquema.Aluno (
    id_aluno INTEGER PRIMARY KEY,
    email VARCHAR(120)
);
 
ALTER TABLE esquema.Aluno ADD CONSTRAINT uq_email UNIQUE (email);

CREATE TABLE esquema.Curso (
    id_curso INTEGER PRIMARY KEY,
    nome VARCHAR(80)
);

CREATE TABLE esquema.Matricula (
    id_matricula INTEGER,
    id_aluno INTEGER,
    id_curso INTEGER
);
 
ALTER TABLE Matricula ADD CONSTRAINT esquema.pk_matricula PRIMARY KEY (id_matricula);
 
ALTER TABLE esquema.Matricula ADD CONSTRAINT esquema.FK_Matricula_2
    FOREIGN KEY (id_aluno)
    REFERENCES esquema.Aluno (id_aluno)
    ON DELETE CASCADE ON UPDATE NO ACTION;
 
ALTER TABLE esquema.Matricula ADD CONSTRAINT esquema.fk_curso
    FOREIGN KEY (id_curso)
    REFERENCES esquema.Curso (id_curso)
    ON DELETE SET NULL;
