#include "core/security/Password.h"

#include <coroutine>
#include <exception>
#include <functional>
#include <optional>
#include <stdexcept>

#include <sodium.h>
#include <trantor/utils/ConcurrentTaskQueue.h>

namespace core::security
{
namespace
{
std::string dummy;

trantor::ConcurrentTaskQueue &workers()
{
    static trantor::ConcurrentTaskQueue queue(4, "password");
    return queue;
}

template <typename T>
class RunOnWorker
{
  public:
    explicit RunOnWorker(std::function<T()> work) : work_(std::move(work))
    {
    }

    bool await_ready() const noexcept
    {
        return false;
    }

    void await_suspend(std::coroutine_handle<> handle)
    {
        workers().runTaskInQueue([this, handle] {
            try
            {
                result_ = work_();
            }
            catch (...)
            {
                error_ = std::current_exception();
            }
            handle.resume();
        });
    }

    T await_resume()
    {
        if (error_)
            std::rethrow_exception(error_);
        return std::move(*result_);
    }

  private:
    std::function<T()> work_;
    std::optional<T> result_;
    std::exception_ptr error_;
};

std::string hashNow(const std::string &password)
{
    char out[crypto_pwhash_STRBYTES];
    if (crypto_pwhash_str(out,
                          password.data(),
                          password.size(),
                          crypto_pwhash_OPSLIMIT_INTERACTIVE,
                          crypto_pwhash_MEMLIMIT_INTERACTIVE) != 0)
        throw std::runtime_error("password hashing ran out of memory");
    return out;
}
}

void init()
{
    if (sodium_init() < 0)
        throw std::runtime_error("libsodium failed to initialise");
    dummy = hashNow("miron-dummy-password");
}

drogon::Task<std::string> hashPassword(std::string password)
{
    co_return co_await RunOnWorker<std::string>([password = std::move(password)] { return hashNow(password); });
}

drogon::Task<bool> verifyPassword(std::string hash, std::string password)
{
    co_return co_await RunOnWorker<bool>([hash = std::move(hash), password = std::move(password)] {
        return crypto_pwhash_str_verify(hash.c_str(), password.data(), password.size()) == 0;
    });
}

const std::string &dummyHash()
{
    return dummy;
}
}
