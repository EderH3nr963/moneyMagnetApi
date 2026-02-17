package com.moneyMagnetApi.demo.service;

import com.moneyMagnetApi.demo.domain.category.Category;
import com.moneyMagnetApi.demo.domain.transaction.Transaction;
import com.moneyMagnetApi.demo.domain.usuario.Usuario;
import com.moneyMagnetApi.demo.dto.request.*;
import com.moneyMagnetApi.demo.dto.response.PageTransactionResponseDTO;
import com.moneyMagnetApi.demo.dto.response.TransactionImportResponseDTO;
import com.moneyMagnetApi.demo.dto.response.TransactionResponseDTO;
import com.moneyMagnetApi.demo.repository.CategoryRepository;
import com.moneyMagnetApi.demo.repository.TransactionRepository;
import com.moneyMagnetApi.demo.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final ExcelService excelService;
    private final UsuarioRepository usuarioRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            ExcelService excelService,
            UsuarioRepository usuarioRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.excelService = excelService;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public PageTransactionResponseDTO getAll(UUID usuarioId, Pageable pageable) {
        Page<Transaction> pageTransaction =
                transactionRepository.findByUsuarioId(usuarioId, pageable);

        return PageTransactionResponseDTO.from(pageTransaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponseDTO getById(UUID usuarioId, UUID transactionId) {

        Transaction transaction = findTransactionOrThrow(usuarioId, transactionId);

        return TransactionResponseDTO.fromTransaction(transaction);
    }

    @Transactional
    public TransactionImportResponseDTO importXlsx(UUID usuarioId, MultipartFile file) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        try {
            var response = excelService.readExcel(usuario, file);

            if (!response.transactions().isEmpty()) {
                transactionRepository.saveAll(response.transactions());
            }

            return new TransactionImportResponseDTO(
                    response.transactions().size(),
                    response.transactions()
                            .stream()
                            .map(TransactionResponseDTO::fromTransaction)
                            .toList(),
                    response.errors()
            );

        } catch (IOException e) {
            throw new RuntimeException("Erro ao processar o arquivo Excel informado", e);
        }
    }

    @Transactional
    public TransactionResponseDTO create(UUID usuarioId, CreateTransactionDTO dto) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        Category category = categoryRepository
                .findByIdAndUsuarioId(dto.category_id(), usuarioId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Categoria selecionada não encontrada para o usuário informado")
                );

        Transaction transaction = new Transaction();
        transaction.setDescription(dto.description());
        transaction.setDate(dto.date());
        transaction.setAmount(dto.amount());
        transaction.setCategory(category);
        transaction.setUsuario(usuario);

        transactionRepository.save(transaction);

        return TransactionResponseDTO.fromTransaction(transaction);
    }

    @Transactional
    public TransactionResponseDTO update(
            UUID usuarioId,
            UUID transactionId,
            UpdateTransactionDTO dto
    ) {
        Transaction transaction = findTransactionOrThrow(usuarioId, transactionId);
        Category category = categoryRepository
                .findByIdAndUsuarioId(dto.category_id(), usuarioId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Categoria selecionada não encontrada para o usuário informado")
                );

        transaction.setDescription(dto.description());
        transaction.setDate(dto.date());
        transaction.setAmount(dto.amount());
        transaction.setCategory(category);

        return TransactionResponseDTO.fromTransaction(transaction);
    }

    @Transactional
    public TransactionResponseDTO updateDescription(
            UUID usuarioId,
            UUID transactionId,
            UpdateDescriptionTransactionDTO dto
    ) {

        Transaction transaction = findTransactionOrThrow(usuarioId, transactionId);

        transaction.setDescription(dto.description());

        return TransactionResponseDTO.fromTransaction(transaction);
    }

    @Transactional
    public TransactionResponseDTO updateAmount(
            UUID usuarioId,
            UUID transactionId,
            UpdateAmountTransactionDTO dto
    ) {

        Transaction transaction = findTransactionOrThrow(usuarioId, transactionId);

        transaction.setAmount(dto.amount());

        return TransactionResponseDTO.fromTransaction(transaction);
    }

    @Transactional
    public TransactionResponseDTO updateDate(
            UUID usuarioId,
            UUID transactionId,
            UpdateDateTransactionDTO dto
    ) {

        Transaction transaction = findTransactionOrThrow(usuarioId, transactionId);

        transaction.setDate(dto.date());

        return TransactionResponseDTO.fromTransaction(transaction);
    }

    @Transactional
    public void delete(UUID usuarioId, UUID transactionId) {

        Transaction transaction = findTransactionOrThrow(usuarioId, transactionId);

        transactionRepository.delete(transaction);
    }

    private Transaction findTransactionOrThrow(UUID usuarioId, UUID transactionId) {
        return transactionRepository
                .findByIdAndUsuarioId(transactionId, usuarioId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Transação não encontrada para o usuário informado")
                );
    }
}
