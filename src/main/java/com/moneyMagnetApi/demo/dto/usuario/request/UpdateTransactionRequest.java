package com.moneyMagnetApi.demo.dto.usuario.request;
import java.util.*;

public record UpdateTransactionRequest(
        String description,
        UUID categoryId
) {
}