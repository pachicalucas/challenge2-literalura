package br.com.alura.literalura.principal;

import br.com.alura.literalura.models.Autor;
import br.com.alura.literalura.models.Livro;
import br.com.alura.literalura.models.LivroDTO;
import br.com.alura.literalura.repositories.LivroRepository;
import br.com.alura.literalura.services.ConsumoAPI;
import br.com.alura.literalura.services.DataConverter;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class Principal {

    @Autowired
    private LivroRepository livroRepository;

    @Autowired
    private ConsumoAPI consumoAPI;

    @Autowired
    private DataConverter converteDados;

    private final Scanner leitura = new Scanner(System.in);

    public Principal(LivroRepository livroRepository, ConsumoAPI consumoAPI, DataConverter converteDados) {
        this.livroRepository = livroRepository;
        this.consumoAPI = consumoAPI;
        this.converteDados = converteDados;
    }

    public void executar() {
        boolean running = true;
        while (running) {
            exibirMenu();
            int opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1 -> buscarLivrosPeloTitulo();
                case 2 -> listarLivrosRegistrados();
                case 3 -> listarAutoresRegistrados();
                case 4 -> listarAutoresVivos();
                case 5 -> listarAutoresVivosRefinado();
                case 6 -> listarAutoresPorAnoDeMorte();
                case 7 -> listarLivrosPorIdioma();
                case 0 -> {
                    System.out.println("Encerrando a LiterAlura!");
                    running = false;
                }
                default -> System.out.println("Opção inválida!");
            }
        }
    }

    private void exibirMenu() {
        System.out.println("""
                ╔══════════════════════════════════════════════════════════╗
                ║                         LITERALURA                       ║
                ║----------------------------------------------------------║
                ║                Sistema para amantes de livros!           ║
                ║                                                          ║
                ║                       Escolha uma opção:                 ║
                ║----------------------------------------------------------║
                ║  1 - Buscar livros por título.                           ║
                ║  2 - Listar todos os livros registrados.                 ║
                ║  3 - Listar todos os autores registrados.                ║
                ║  4 - Listar todos os autores vivos em um determinado ano.║
                ║  5 - Listar todos os autores nascidos em determinado ano.║
                ║  6 - Listar todos os autores por ano de morte.           ║
                ║  7 - Listar todos os livros em um determinado idioma.    ║
                ║  0 - Sair.                                               ║
                ╚══════════════════════════════════════════════════════════╝
                """);
    }

    private void salvarLivros(List<Livro> livros) {
        livros.forEach(livroRepository::save);
    }

    private void buscarLivrosPeloTitulo() {
        String baseURL = "https://gutendex.com/books?search=";

        try {
            System.out.println("Digite o título do livro: ");
            String titulo = leitura.nextLine();
            String endereco = baseURL + titulo.replace(" ", "%20");
            System.out.println("URL da API: " + endereco);

            String jsonResponse = consumoAPI.obterDadosAPI(endereco);
            processarRespostaAPI(jsonResponse, titulo);

        } catch (Exception e) {
            System.out.println("Erro ao buscar livros: " + e.getMessage());
        }
    }

    private void processarRespostaAPI(String jsonResponse, String titulo) {
        try {
            if (jsonResponse.isEmpty()) {
                System.out.println("Resposta da API está vazia.");
                return;
            }

            JsonNode rootNode = converteDados.getObjectMapper().readTree(jsonResponse);
            JsonNode resultsNode = rootNode.path("results");

            if (resultsNode.isEmpty()) {
                System.out.println("Não foi possível encontrar o livro buscado.");
                return;
            }

            List<LivroDTO> livrosDTO = converteDados.getObjectMapper()
                    .readerForListOf(LivroDTO.class)
                    .readValue(resultsNode);

            removerDuplicatasEInserirNovosLivros(livrosDTO, titulo);

        } catch (Exception e) {
            System.out.println("Erro ao processar resposta da API: " + e.getMessage());
        }
    }

    private void removerDuplicatasEInserirNovosLivros(List<LivroDTO> livrosDTO, String titulo) {
        List<Livro> livrosExistentes = livroRepository.findByTitulo(titulo);
        if (!livrosExistentes.isEmpty()) {
            System.out.println("Removendo livros duplicados já existentes no banco de dados...");
            for (Livro livroExistente : livrosExistentes) {
                livrosDTO.removeIf(livroDTO -> livroExistente.getTitulo().equals(livroDTO.titulo()));
            }
        }

        if (!livrosDTO.isEmpty()) {
            List<Livro> novosLivros = livrosDTO.stream().map(Livro::new).collect(Collectors.toList());
            salvarLivros(novosLivros);
            System.out.println("Livros salvos com sucesso!");
        } else {
            System.out.println("Todos os livros já estão registrados no banco de dados.");
        }

        exibirLivrosEncontrados(livrosDTO);
    }

    private void exibirLivrosEncontrados(List<LivroDTO> livrosDTO) {
        if (!livrosDTO.isEmpty()) {
            System.out.println("Livros encontrados:");
            Set<String> titulosExibidos = new HashSet<>();
            for (LivroDTO livro : livrosDTO) {
                if (titulosExibidos.add(livro.titulo())) {
                    System.out.println(livro);
                }
            }
        }
    }

    private void listarLivrosRegistrados() {
        List<Livro> livros = livroRepository.findAll();
        if (livros.isEmpty()) {
            System.out.println("Nenhum livro registrado.");
        } else {
            livros.forEach(System.out::println);
        }
    }

    private void listarAutoresRegistrados() {
        List<Autor> autores = livroRepository.findAll().stream()
                .map(Livro::getAutor)
                .distinct()
                .collect(Collectors.toList());

        if (autores.isEmpty()) {
            System.out.println("Nenhum autor registrado.");
        } else {
            autores.forEach(autor -> System.out.println(autor.getAutor()));
        }
    }

    private void listarAutoresVivos() {
        System.out.println("Digite o ano: ");
        Integer ano = leitura.nextInt();
        leitura.nextLine();

        Year year = Year.of(ano);
        List<Autor> autores = livroRepository.findAutoresVivos(year);

        if (autores.isEmpty()) {
            System.out.println("Nenhum autor vivo encontrado.");
        } else {
            System.out.println("Lista de autores vivos no ano de " + ano + ":\n");
            autores.forEach(this::exibirAutor);
        }
    }

    private void listarAutoresVivosRefinado() {
        System.out.println("Digite o ano: ");
        Integer ano = leitura.nextInt();
        leitura.nextLine();

        Year year = Year.of(ano);
        List<Autor> autores = livroRepository.findAutoresVivosRefinado(year);

        if (autores.isEmpty()) {
            System.out.println("Nenhum autor foi encontrado.");
        } else {
            System.out.println("Lista de autores nascidos no ano de " + ano + ":\n");
            autores.forEach(this::exibirAutor);
        }
    }

    private void listarAutoresPorAnoDeMorte() {
        System.out.println("Digite o ano: ");
        Integer ano = leitura.nextInt();
        leitura.nextLine();

        Year year = Year.of(ano);
        List<Autor> autores = livroRepository.findAutoresPorAnoDeMorte(year);

        if (autores.isEmpty()) {
            System.out.println("Nenhum autor foi encontrado.");
        } else {
            System.out.println("Lista de autores que morreram no ano de " + ano + ":\n");
            autores.forEach(this::exibirAutor);
        }
    }

    private void listarLivrosPorIdioma() {
        System.out.println("""
                Digite o idioma pretendido:
                Inglês:    en
                Português: pt
                Espanhol:  es
                Francês:   fr
                Alemão:    de
                """);
        String idioma = leitura.nextLine();

        List<Livro> livros = livroRepository.findByIdioma(idioma);
        if (livros.isEmpty()) {
            System.out.println("Nenhum livro encontrado com o idioma informado.");
        } else {
            livros.forEach(this::exibirLivro);
        }
    }

    private void exibirLivro(Livro livro) {
        System.out.println("Título: " + livro.getTitulo());
        System.out.println("Autor: " + livro.getAutor().getAutor());
        System.out.println("Idioma: " + livro.getIdioma());
        System.out.println("----------------------------------------");
    }

    private void exibirAutor(Autor autor) {
        if (Autor.possuiAno(autor.getAnoNascimento()) && Autor.possuiAno(autor.getAnoFalecimento())) {
            String nomeAutor = autor.getAutor();
            String anoNascimento = autor.getAnoNascimento().toString();
            String anoFalecimento = autor.getAnoFalecimento().toString();
            System.out.println(nomeAutor + " (" + anoNascimento + " - " + anoFalecimento + ")");
        }
    }
}