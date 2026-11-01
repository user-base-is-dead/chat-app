#pragma once

#include <cstdint>
#include <optional>
#include <string>
#include <vector>

#include <drogon/utils/coroutine.h>

#include "features/messages/Message.h"

namespace features::messages
{
class MessageRepository
{
  public:
    drogon::Task<std::vector<Message>> list(int64_t chatId, int64_t beforeId, int limit);
    drogon::Task<std::vector<Message>> listAfter(int64_t chatId, int64_t afterId, int limit);
    drogon::Task<Message> insert(int64_t id, int64_t chatId, int64_t senderId, int64_t deviceId, std::string ciphertext);
    drogon::Task<std::optional<Message>> edit(int64_t chatId, int64_t id, int64_t senderId, std::string ciphertext);
    drogon::Task<bool> remove(int64_t chatId, int64_t id);
};
}
