<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import Card from 'primevue/card'
import InputText from 'primevue/inputtext'
import Dropdown from 'primevue/dropdown'
import MultiSelect from 'primevue/multiselect'
import Button from 'primevue/button'
import Message from 'primevue/message'
import Divider from 'primevue/divider'

const authStore = useAuthStore()

// Form state
const displayName = ref('')
const nativeLanguage = ref('')
const learningLanguages = ref<string[]>([])

// UI state
const isEditing = ref(false)
const saveSuccess = ref(false)

// Language options (common languages)
const languageOptions = [
  { label: 'English', value: 'en' },
  { label: 'Spanish', value: 'es' },
  { label: 'French', value: 'fr' },
  { label: 'German', value: 'de' },
  { label: 'Italian', value: 'it' },
  { label: 'Portuguese', value: 'pt' },
  { label: 'Russian', value: 'ru' },
  { label: 'Chinese', value: 'zh' },
  { label: 'Japanese', value: 'ja' },
  { label: 'Korean', value: 'ko' },
  { label: 'Arabic', value: 'ar' },
  { label: 'Hindi', value: 'hi' },
  { label: 'Dutch', value: 'nl' },
  { label: 'Polish', value: 'pl' },
  { label: 'Turkish', value: 'tr' },
  { label: 'Swedish', value: 'sv' },
  { label: 'Norwegian', value: 'no' },
  { label: 'Danish', value: 'da' },
  { label: 'Finnish', value: 'fi' },
  { label: 'Greek', value: 'el' },
]

const availableLearningLanguages = computed(() => {
  return languageOptions.filter(lang => lang.value !== nativeLanguage.value)
})

const getLanguageLabel = (code: string) => {
  const lang = languageOptions.find(l => l.value === code)
  return lang ? lang.label : code
}

// Initialize form with current user data
onMounted(() => {
  if (authStore.user) {
    displayName.value = authStore.user.displayName || ''
    nativeLanguage.value = authStore.user.nativeLanguage
    learningLanguages.value = [...authStore.user.learningLanguages]
  }
})

const handleEdit = () => {
  isEditing.value = true
  saveSuccess.value = false
}

const handleCancel = () => {
  // Reset form to current user data
  if (authStore.user) {
    displayName.value = authStore.user.displayName || ''
    nativeLanguage.value = authStore.user.nativeLanguage
    learningLanguages.value = [...authStore.user.learningLanguages]
  }
  isEditing.value = false
  saveSuccess.value = false
}

const handleSave = async () => {
  const success = await authStore.updateProfile({
    displayName: displayName.value || null,
    nativeLanguage: nativeLanguage.value,
    learningLanguages: learningLanguages.value,
  })

  if (success) {
    isEditing.value = false
    saveSuccess.value = true
    setTimeout(() => {
      saveSuccess.value = false
    }, 3000)
  }
}
</script>

<template>
  <div class="profile-container">
    <div class="profile-content">
      <h1 class="flex items-center gap-md text-4xl font-bold text-primary mb-xl">
        <i class="pi pi-user text-3xl icon-primary"></i>
        My Profile
      </h1>

      <Message v-if="saveSuccess" severity="success" :closable="false" class="success-message">
        Profile updated successfully!
      </Message>

      <Message v-if="authStore.error" severity="error" :closable="false" class="error-message">
        {{ authStore.error }}
      </Message>

      <Card class="profile-card">
        <template #content>
          <div class="profile-info">
            <!-- Email (Read-only) -->
            <div class="info-section">
              <div class="section-header">
                <i class="pi pi-envelope text-xl icon-primary"></i>
                <h3 class="text-xl font-semibold text-primary">Email Address</h3>
              </div>
              <div class="info-field">
                <label>Email</label>
                <InputText
                  :model-value="authStore.user?.email"
                  disabled
                  class="w-full"
                />
                <small class="text-sm text-secondary helper-text">Email cannot be changed</small>
              </div>
            </div>

            <Divider />

            <!-- Personal Information -->
            <div class="info-section">
              <div class="section-header">
                <i class="pi pi-id-card text-xl icon-primary"></i>
                <h3 class="text-xl font-semibold text-primary">Personal Information</h3>
              </div>

              <div class="info-field">
                <label for="displayName">Display Name</label>
                <InputText
                  id="displayName"
                  v-model="displayName"
                  :disabled="!isEditing"
                  placeholder="Enter your display name"
                  class="w-full"
                />
              </div>
            </div>

            <Divider />

            <!-- Language Settings -->
            <div class="info-section">
              <div class="section-header">
                <i class="pi pi-language text-xl icon-primary"></i>
                <h3 class="text-xl font-semibold text-primary">Language Preferences</h3>
              </div>

              <div class="info-field">
                <label for="nativeLanguage">Native Language</label>
                <Dropdown
                  id="nativeLanguage"
                  v-model="nativeLanguage"
                  :options="languageOptions"
                  option-label="label"
                  option-value="value"
                  :disabled="!isEditing"
                  placeholder="Select your native language"
                  class="w-full"
                />
              </div>

              <div class="info-field">
                <label for="learningLanguages">Learning Languages</label>
                <MultiSelect
                  id="learningLanguages"
                  v-model="learningLanguages"
                  :options="availableLearningLanguages"
                  option-label="label"
                  option-value="value"
                  :disabled="!isEditing"
                  placeholder="Select languages you're learning"
                  class="w-full"
                  display="chip"
                />
                <small class="text-sm text-secondary helper-text">Select the languages you want to learn</small>
              </div>
            </div>

            <!-- Action Buttons -->
            <div class="actions">
              <template v-if="!isEditing">
                <Button
                  label="Edit Profile"
                  icon="pi pi-pencil"
                  @click="handleEdit"
                />
              </template>
              <template v-else>
                <Button
                  label="Cancel"
                  icon="pi pi-times"
                  severity="secondary"
                  outlined
                  @click="handleCancel"
                />
                <Button
                  label="Save Changes"
                  icon="pi pi-check"
                  :loading="authStore.loading"
                  @click="handleSave"
                />
              </template>
            </div>
          </div>
        </template>
      </Card>

      <!-- Account Information -->
      <Card class="info-card">
        <template #content>
          <div class="flex flex-col gap-md">
            <div class="info-item">
              <span class="flex items-center gap-sm font-medium text-secondary">
                <i class="pi pi-shield icon-primary"></i>
                Account Type
              </span>
              <span class="font-semibold text-primary">
                {{ authStore.user?.roles.join(', ') }}
              </span>
            </div>
            <div class="info-item">
              <span class="flex items-center gap-sm font-medium text-secondary">
                <i class="pi pi-book icon-primary"></i>
                Native Language
              </span>
              <span class="font-semibold text-primary">
                {{ getLanguageLabel(authStore.user?.nativeLanguage || 'en') }}
              </span>
            </div>
            <div class="info-item">
              <span class="flex items-center gap-sm font-medium text-secondary">
                <i class="pi pi-globe icon-primary"></i>
                Learning
              </span>
              <span class="font-semibold text-primary">
                {{ authStore.user?.learningLanguages.map(getLanguageLabel).join(', ') || 'None' }}
              </span>
            </div>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<style scoped>
.profile-container {
  min-height: 100vh;
  background: var(--bg-primary);
  padding: 2rem 1rem;
}

.profile-content {
  max-width: 900px;
  margin: 0 auto;
}

.success-message,
.error-message {
  margin-bottom: 1.5rem;
}

.profile-card {
  margin-bottom: 1.5rem;
}

.profile-info {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.info-section {
  padding: 0.5rem 0;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
}

.info-field {
  margin-bottom: 1.25rem;
}

.info-field:last-child {
  margin-bottom: 0;
}

.info-field label {
  display: block;
  font-weight: 500;
  margin-bottom: 0.5rem;
  color: var(--text-primary);
}

.w-full {
  width: 100%;
}

.actions {
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--border-light);
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
}

.info-card {
  background: var(--bg-secondary);
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

.icon-primary {
  color: var(--primary);
}

.helper-text {
  display: block;
  margin-top: 0.25rem;
}

@media (max-width: 768px) {
  .profile-container {
    padding: 1rem;
  }

  h1 {
    font-size: 2rem;
  }

  .actions {
    flex-direction: column;
  }

  .actions button {
    width: 100%;
  }

  .info-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }
}
</style>
