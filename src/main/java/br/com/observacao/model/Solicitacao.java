package br.com.observacao.model;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "solicitacoes")
public class Solicitacao {

    @Id
    private String codigo;

    @NotBlank
    private String categoria;

    @NotBlank
    private String prioridade;

    @NotBlank
    private String descricao;

    private String status;
    private String localizacao;
    private String data;
    private String sla;
    private boolean anonimo;
    private String nomeCidadao;
    private String emailCidadao;

    public Solicitacao() { }

    public String getCodigo() {
        return codigo;
    }
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getCategoria() {
        return categoria;
    }
    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getPrioridade() {
        return prioridade;
    }
    public void setPrioridade(String prioridade) {
        this.prioridade = prioridade;
    }

    public String getDescricao() {
        return descricao;
    }
    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public String getLocalizacao() {
        return localizacao;
    }
    public void setLocalizacao(String localizacao) {
        this.localizacao = localizacao;
    }

    public String getData() {
        return data;
    }
    public void setData(String data) {
        this.data = data;
    }

    public String getSla() {
        return sla;
    }
    public void setSla(String sla) {
        this.sla = sla;
    }

    public boolean isAnonimo() {
        return anonimo;
    }
    public void setAnonimo(boolean anonimo) {
        this.anonimo = anonimo;
    }

    public String getNomeCidadao() {
        return nomeCidadao;
    }
    public void setNomeCidadao(String nomeCidadao) {
        this.nomeCidadao = nomeCidadao;
    }

    public String getEmailCidadao() {
        return emailCidadao;
    }
    public void setEmailCidadao(String emailCidadao) {
        this.emailCidadao = emailCidadao;
    }
}
