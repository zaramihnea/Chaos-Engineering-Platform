package com.example.cep.controlplane.service;
public interface ApprovalService {
    String request(String experimentId, String requester);
    String approve(String experimentId, String approver);
    String reject(String experimentId, String approver, String reason);
}