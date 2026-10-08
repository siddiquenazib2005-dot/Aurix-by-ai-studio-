package com.example.aurix.data.model

enum class MissionStatus {
    PENDING,
    IN_PROGRESS,
    AWAITING_APPROVAL,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class StepStatus {
    PENDING,
    EXECUTING,
    VERIFYING,
    SUCCESS,
    FAILED,
    SKIPPED
}

data class MissionStep(
    val stepIndex: Int,
    val title: String,
    val toolName: String,
    val argumentsJson: String,
    var status: StepStatus = StepStatus.PENDING,
    var realityVerificationReport: String? = null,
    var errorReason: String? = null,
    var executionResult: String? = null,
    var retryCount: Int = 0
)

enum class AutonomyMode {
    FULL_MANUAL,        // Ask approval for every tool execution
    ASK_FOR_SENSITIVE,  // Ask for calls, SMS, deletion, payments
    SAFE_AUTO           // Auto-execute safe & read tools, ask only for critical external writes
}

enum class MemoryCategory {
    WORKING_MEMORY,
    CONVERSATION_MEMORY,
    USER_PREFERENCE,
    IMPORTANT_FACT,
    TASK_HISTORY,
    SEMANTIC_MEMORY,
    LEARNED_WORKFLOW,
    PROJECT_CONTEXT
}
