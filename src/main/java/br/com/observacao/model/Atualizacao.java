package br.com.observacao.model;

public class Atualizacao {
    private String dataHora;
    private String nota;
    private String autor;

    public Atualizacao() {}

    public Atualizacao(String dataHora, String nota, String autor) {
        this.dataHora = dataHora;
        this.nota = nota;
        this.autor = autor;
    }

    public String getDataHora() { return dataHora; }
    public void setDataHora(String dataHora) { this.dataHora = dataHora; }

    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota = nota; }

    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }
}