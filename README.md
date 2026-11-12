# AEP 2026 - SEGUNDO SEMESTRE
> ObservaAção

## Definição do Problema
O ObservaAção é um sistema de zeladoria urbana focado no cidadão.
O objetivo é solucionar a dificuldade de comunicação entre a população e os órgãos públicos, facilitando o registro de demandas da cidade (como buracos na via, falta de iluminação ou problemas na limpeza urbana).
A aplicação busca dar transparência a esse processo de atendimento.
O projeto foi desenhado com base no **ODS 16** (Paz, Justiça e Instituições Eficazes) da ONU, garantindo que as instituições públicas sejam mais responsivas, acessíveis e transparentes para a sociedade.

## Evolução do Banco de Dados (2º Semestre)
Para atender aos requisitos finais da disciplina, a modelagem no MongoDB foi evoluída para suportar **objetos complexos, subdocumentos e múltiplas coleções com relacionamentos**:
- **Objetos Aninhados:** A coleção principal `solicitacoes` agora embute a classe `Endereco` (logradouro, bairro, ponto de referência), estruturando os dados de localização.
- **Listas de Subdocumentos:** Foi adicionada uma lista de objetos `Atualizacao` (`historico`) para registrar os eventos do chamado de forma aninhada ao documento principal.
- **Múltiplas Coleções e Relacionamentos:** Os dados dos munícipes foram extraídos para uma coleção independente chamada `usuarios`. O relacionamento entre a solicitação e o usuário é mantido via referência (ID).

## Funcionalidades
Esta Prova de Conceito (PoC) final funciona via linha de comando (CLI) e contempla as operações de CRUD evoluídas no banco de dados NoSQL:

- **Registrar Solicitação (Create):** Criação de chamados informando categoria, prioridade, descrição e endereço estruturado. O sistema atrela a solicitação ao perfil do cidadão, gera um protocolo único e define o SLA.
- **Listar Solicitações (Read):** Exibição geral de todos os chamados ativos no sistema para acompanhamento rápido.
- **Ver Detalhes da Solicitação (Read):** Consulta completa de todas as informações, dados do usuário criador e o histórico de atualizações de um chamado específico.
- **Atualizar Status (Update):** Modificação do andamento de uma solicitação existente gerando novos registros no histórico aninhado.
- **Excluir Solicitação (Delete):** Remoção permanente de chamados do banco de dados.

## Tecnologias utilizadas

- **Java 26**: linguagem utilizada no desenvolvimento da aplicação orientada a objetos.
- **Spring Boot 4.1.1**: estrutura principal para configuração e execução da aplicação.
- **Spring Data MongoDB**: integração avançada entre a aplicação Java e o banco de dados NoSQL (suporte a `@Document` e referências).
- **MongoDB 8.0**: armazenamento das entidades nas coleções `solicitacoes` e `usuarios`.
- **Maven Wrapper**: gerenciamento das dependências e execução do projeto.
- **Docker e Docker Compose**: criação e inicialização padronizada do ambiente MongoDB.
- **Jakarta Bean Validation**: validação dos dados obrigatórios das solicitações.
- **Lombok**: suporte à geração de código durante a compilação.
- **JaCoCo**: plugin para validação e geração de relatório de cobertura de código (mínimo de 70%).

## Banco de dados MongoDB

A aplicação utiliza o banco NoSQL MongoDB. Por padrão, a conexão é realizada em `mongodb://localhost:27017/observacao`, gerenciando as coleções `solicitacoes` e `usuarios`.

### Iniciar o banco de dados

Com o Docker Desktop em execução, abra um terminal na pasta do projeto e execute:

```powershell
docker compose up -d
```

Verifique se o contêiner está saudável:

```powershell
docker compose ps
docker exec observacao-mongodb mongosh --quiet --eval "db.adminCommand('ping')"
```

O comando de verificação deve retornar `{ ok: 1 }`.

### Configurar outra conexão

Para utilizar outro servidor MongoDB, defina a variável de ambiente `MONGODB_URI` antes de iniciar a aplicação:

```powershell
$env:MONGODB_URI="mongodb://localhost:27017/observacao"
```

### Consultar as solicitações

Abra o terminal do MongoDB dentro do contêiner:

```powershell
docker exec -it observacao-mongodb mongosh
```

Em seguida, selecione o banco e consulte as coleções:

```javascript
use observacao
db.usuarios.find()
db.solicitacoes.find()
```

### Encerrar o banco de dados

```powershell
docker compose down
```

Os dados permanecem armazenados no volume `mongodb_data` e estarão disponíveis na próxima inicialização.

## Executando os testes e Cobertura (≥ 70%)

Este projeto cumpre o requisito de testes automatizados com cobertura mínima obrigatória de 70%.

### 1. Testes Unitários e Integração

Os testes cobrem a camada de serviços e o menu interativo. O `ObservacaoApplicationTests` sobe o contexto completo do Spring Boot com um MongoDB real via [Testcontainers](https://testcontainers.com/). Ele **requer Docker instalado e em execução** na máquina.

Para executar todos os testes, utilize:
```bash
./mvnw clean test
```

### 2. Relatório de Cobertura (JaCoCo)

Para gerar e visualizar a evidência da cobertura mínima de 70% exigida pela avaliação, execute:
```bash
./mvnw clean test jacoco:report
```
Após a execução com sucesso, acesse o relatório visual abrindo o arquivo abaixo no seu navegador de internet:
`target/site/jacoco/index.html`