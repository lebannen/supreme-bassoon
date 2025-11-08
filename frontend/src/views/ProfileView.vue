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
      <h1 class="profile-title">
        <i class="pi pi-user"></i>
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
                <i class="pi pi-envelope"></i>
                <h3>Email Address</h3>
              </div>
              <div class="info-field">
                <label>Email</label>
                <InputText
                  :model-value="authStore.user?.email"
                  disabled
                  class="w-full"
                />
                <small class="field-hint">Email cannot be changed</small>
              </div>
            </div>

            <Divider />

            <!-- Personal Information -->
            <div class="info-section">
              <div class="section-header">
                <i class="pi pi-id-card"></i>
                <h3>Personal Information</h3>
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
                <i class="pi pi-language"></i>
                <h3>Language Preferences</h3>
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
                <small class="field-hint">Select the languages you want to learn</small>
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
          <div class="account-info">
            <div class="info-item">
              <span class="info-label">
                <i class="pi pi-shield"></i>
                Account Type
              </span>
              <span class="info-value">
                {{ authStore.user?.roles.join(', ') }}
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">
                <i class="pi pi-book"></i>
                Native Language
              </span>
              <span class="info-value">
                {{ getLanguageLabel(authStore.user?.nativeLanguage || 'en') }}
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">
                <i class="pi pi-globe"></i>
                Learning
              </span>
              <span class="info-value">
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
  background: var(--surface-ground);
  padding: 2rem 1rem;
}

.profile-content {
  max-width: 900px;
  margin: 0 auto;
}

.profile-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: var(--text-color);
  margin-bottom: 2rem;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.profile-title i {
  color: var(--primary-color);
  font-size: 2rem;
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

.section-header i {
  color: var(--primary-color);
  font-size: 1.25rem;
}

.section-header h3 {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-color);
  margin: 0;
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
  color: var(--text-color);
}

.field-hint {
  display: block;
  margin-top: 0.25rem;
  color: var(--text-color-secondary);
  font-size: 0.875rem;
}

.w-full {
  width: 100%;
}

.actions {
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--surface-border);
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
}

.info-card {
  background: var(--surface-card);
}

.account-info {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem;
  background: var(--surface-50);
  border-radius: 6px;
}

.info-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 500;
  color: var(--text-color-secondary);
}

.info-label i {
  color: var(--primary-color);
}

.info-value {
  font-weight: 600;
  color: var(--text-color);
}

@media (max-width: 768px) {
  .profile-container {
    padding: 1rem;
  }

  .profile-title {
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
